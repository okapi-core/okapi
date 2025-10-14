package org.okapi.metricsproxy.service;

import static org.okapi.validation.OkapiChecks.checkArgument;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.okapi.auth.AccessManager;
import org.okapi.auth.TokenManager;
import org.okapi.data.dao.TeamsDao;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.ExceptionUtils;
import org.okapi.exceptions.NotFoundException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.http.BatchResponseTranslator;
import org.okapi.http.HttpBatchClient;
import org.okapi.intervals.IntervalUtils;
import org.okapi.metrics.IdCreator;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.scanning.EmptyFileException;
import org.okapi.metrics.scanning.HourlyCheckpointScanner;
import org.okapi.metrics.search.MetricsSearcher;
import org.okapi.metricsproxy.auth.AuthorizationChecker;
import org.okapi.rest.metrics.*;
import org.okapi.rest.metrics.query.GetGaugeRequest;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.metrics.search.SearchMetricsRequest;
import org.okapi.rest.metrics.search.SearchMetricsRequestInternal;
import org.okapi.rest.metrics.search.SearchMetricsResponse;
import org.okapi.s3.S3ByteRangeCache;
import org.okapi.usermessages.UserFacingMessages;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@AllArgsConstructor
@Builder
public class ScanQueryProcessor {
  OkHttpClient okHttpClient;
  Gson gson;
  MetadataCache metadataCache;
  String dataBucket;
  S3ByteRangeCache rangeCache;
  AuthorizationChecker authorizationChecker;
  ZkRegistry zkRegistry;
  S3Client amazonS3;
  TokenManager tokenManager;
  AccessManager accessManager;
  TeamsDao teamsDao;

  public GetMetricsResponse getMetricsResponse(String tempToken, GetGaugeRequest getGaugeRequest)
      throws Exception {
    var userId = tokenManager.getUserId(tempToken);
    var teamId = getGaugeRequest.getTeam();
    var optionalTeamDto = teamsDao.get(teamId);
    checkArgument(
        optionalTeamDto.isPresent(),
        () -> new BadRequestException(UserFacingMessages.TEAM_NOT_FOUND));
    var teamDto = optionalTeamDto.get();
    var orgId = teamDto.getOrgId();
    accessManager.checkUserCanReadFromTeam(userId, orgId, teamId);
    var tenantId = IdCreator.getTenantId(orgId, teamId);
    var getMetricsRequestInternal =
        GetMetricsRequest.builder()
            .tenantId(tenantId)
            .metricName(getGaugeRequest.getMetricName())
            .tags(getGaugeRequest.getTags())
            .start(getGaugeRequest.getStart())
            .end(getGaugeRequest.getEnd())
            .aggregation(getGaugeRequest.getAggregation())
            .resolution(getGaugeRequest.getResolution())
            .build();
    var queryInterval =
        new IntervalUtils.Interval(getGaugeRequest.getStart(), getGaugeRequest.getEnd());
    var admissionWindow = System.currentTimeMillis() - Duration.of(24, ChronoUnit.HOURS).toMillis();
    var onlinePart =
        IntervalUtils.clipAfter(queryInterval.start(), queryInterval.end(), admissionWindow);
    var offlinePart =
        IntervalUtils.clipBefore(queryInterval.start(), queryInterval.end(), admissionWindow);
    Optional<GetMetricsResponse> onlineResult =
        onlinePart.isEmpty()
            ? Optional.empty()
            : processOnlineQuery(
                onlinePart.get().start(),
                onlinePart.get().end(),
                getMetricsRequestInternal.getTenantId(),
                getMetricsRequestInternal.getMetricName(),
                getMetricsRequestInternal.getTags(),
                getMetricsRequestInternal.getResolution(),
                getMetricsRequestInternal.getAggregation());
    List<GetMetricsResponse> offline =
        offlinePart.isEmpty()
            ? Collections.emptyList()
            : processOfflineQueryParts(
                offlinePart.get().start(),
                offlinePart.get().end(),
                getMetricsRequestInternal.getTenantId(),
                getMetricsRequestInternal.getMetricName(),
                getMetricsRequestInternal.getTags(),
                getMetricsRequestInternal.getResolution(),
                getMetricsRequestInternal.getAggregation());

    var response =
        GetMetricsResponse.builder()
            .tenant(tenantId)
            .name(getMetricsRequestInternal.getMetricName())
            .tags(getMetricsRequestInternal.getTags())
            .aggregation(getMetricsRequestInternal.getAggregation())
            .resolution(getMetricsRequestInternal.getResolution());
    var times = new ArrayList<Long>();
    var values = new ArrayList<Float>();
    for (int i = 0; i < offline.size(); i++) {
      times.addAll(offline.get(i).getTimes());
      values.addAll(offline.get(i).getValues());
    }
    if (onlineResult.isPresent()) {
      times.addAll(onlineResult.get().getTimes());
      values.addAll(onlineResult.get().getValues());
    }
    return response.times(times).values(values).build();
  }

  public Optional<GetMetricsResponse> processOnlineQuery(
      long start,
      long end,
      String tenantId,
      String metricName,
      Map<String, String> tags,
      RES_TYPE resType,
      AGG_TYPE aggType)
      throws Exception {
    if (end < start) return Optional.empty();
    var path = MetricPaths.convertToUnivPath(tenantId, metricName, tags);
    var node = zkRegistry.route(path);
    var getQuery =
        GetMetricsRequest.builder()
            .start(start)
            .end(end)
            .tenantId(tenantId)
            .metricName(metricName)
            .tags(tags)
            .aggregation(aggType)
            .resolution(resType)
            .build();
    var body = gson.toJson(getQuery);
    var requestBody = RequestBody.create(body.getBytes(StandardCharsets.UTF_8));
    var url = "http://" + node.ip() + "/api/v1/metrics/q";
    var request =
        new Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build();
    try (var res = okHttpClient.newCall(request).execute()) {
      if (res.isSuccessful()) {
        var responseBody = res.body();
        if (responseBody == null) {
          throw new IllegalStateException("Response body is null, this is not accepted.");
        }
        return Optional.ofNullable(gson.fromJson(responseBody.string(), GetMetricsResponse.class));
      } else {
        throw new IllegalArgumentException("Request has failed.");
      }
    }
  }

  public List<GetMetricsResponse> processOfflineQueryParts(
      long start,
      long end,
      String tenantId,
      String metricName,
      Map<String, String> tags,
      RES_TYPE resType,
      AGG_TYPE aggType)
      throws IOException, ExecutionException, StreamReadingException {
    // this is a multistep query, it should be broken into hourly parts
    if (end < start) {
      return Collections.emptyList();
    }
    var hourStart = start / 1000 / 3600;
    var hourEnd = end / 1000 / 3600;
    var results = new ArrayList<GetMetricsResponse>();
    for (long hr = hourStart; hr <= hourEnd; hr++) {
      var result = processHourlyQuery(hr, tenantId, metricName, tags, resType, aggType);
      result.ifPresent(r -> results.add(r));
    }
    return results;
  }

  public Optional<GetMetricsResponse> processHourlyQuery(
      long hour,
      String tenantId,
      String metricName,
      Map<String, String> tags,
      RES_TYPE resType,
      AGG_TYPE aggType)
      throws StreamReadingException, IOException, ExecutionException {
    var path = MetricPaths.convertToUnivPath(tenantId, metricName, tags);
    var matchingPrefix =
        metadataCache.getPrefix(
            path, hour, tenantId); // check which metric path is correct based on the cache.
    if (matchingPrefix.isEmpty()) {
      return Optional.empty();
    }

    var brs = new S3ByteRangeScanner(dataBucket, matchingPrefix.get(), amazonS3, rangeCache);
    var scanner = new HourlyCheckpointScanner();
    Map<String, List<Long>> md = null;
    try {
      md = scanner.getMd(brs);
    } catch (EmptyFileException e) {
      log.error("Found empty file where not expected. {}", ExceptionUtils.debugFriendlyMsg(e));
      return Optional.empty();
    }
    var startTime = hour * 3600 * 1000;
    var responseBuilder =
        GetMetricsResponse.builder()
            .aggregation(aggType)
            .resolution(resType)
            .name(metricName)
            .tags(tags);
    switch (resType) {
      case SECONDLY:
        {
          var reading = scanner.secondly(brs, path, md);
          var times = reading.getTs().stream().map(second -> startTime + second * 1000).toList();
          var values =
              reading.getVals().stream().map(statistics -> statistics.aggregate(aggType)).toList();
          return Optional.of(responseBuilder.values(values).times(times).build());
        }
      case MINUTELY:
        {
          var reading = scanner.minutely(brs, path, md);
          var times = reading.getTs().stream().map(min -> startTime + min * 60 * 1000).toList();
          var values =
              reading.getVals().stream().map(statistics -> statistics.aggregate(aggType)).toList();
          return Optional.of(responseBuilder.values(values).times(times).build());
        }
      case HOURLY:
        {
          var reading = scanner.hourly(brs, path, md);
          var value = reading.aggregate(aggType);
          return Optional.of(
              responseBuilder.values(Arrays.asList(value)).times(Arrays.asList(startTime)).build());
        }
      default:
        throw new IllegalArgumentException("Unrecognized resolution.");
    }
  }

  public boolean acceptableWindowSize(SearchMetricsRequest req) {
    return req.getEndTime() - req.getStartTime() < Duration.of(7, ChronoUnit.DAYS).toMillis();
  }

  public SearchMetricsResponse searchMetrics(
      String tempToken, SearchMetricsRequest searchMetricsRequest)
      throws UnAuthorizedException, BadRequestException, NotFoundException {
    var userId = tokenManager.getUserId(tempToken);
    var team = searchMetricsRequest.getTeam();
    var teamDto = teamsDao.get(team);
    checkArgument(teamDto.isPresent(), UnAuthorizedException::new);
    accessManager.checkUserCanReadFromTeam(
        userId, teamDto.get().getOrgId(), teamDto.get().getTeamId());

    var orgId = teamDto.get().getOrgId();
    var teamId = teamDto.get().getTeamId();
    checkArgument(acceptableWindowSize(searchMetricsRequest), BadRequestException::new);
    var tenantId = IdCreator.getTenantId(orgId, teamId);
    var internalRequest =
        SearchMetricsRequestInternal.builder()
            .pattern(searchMetricsRequest.getPattern())
            .startTime(searchMetricsRequest.getStartTime())
            .endTime(searchMetricsRequest.getEndTime())
            .tenantId(tenantId);
    var body = gson.toJson(internalRequest);
    var batchClient = HttpBatchClient.create(okHttpClient);
    var cutoffWindow = System.currentTimeMillis() - Duration.of(24, ChronoUnit.HOURS).toMillis();
    var nodes = zkRegistry.listNodes().stream().map(node -> "http://" + node.ip()).toList();
    var results =
        batchClient
            .broadcast(
                nodes,
                HttpBatchClient.HttpMethod.POST,
                "api/v1/metrics/s",
                Map.of("Content-Type", "application/json"),
                RequestBody.create(body.getBytes(StandardCharsets.UTF_8)))
            .executeAll();
    var counter = new MetricsPathCounter();
    var translated = BatchResponseTranslator.translate(results, SearchMetricsResponse.class, gson);
    var serverErrorCount = 0;
    var errors = new ArrayList<String>(translated.clientErrors());
    for (var response : translated.results()) {
      if (response.getClientErrors() != null) {
        errors.addAll(response.getClientErrors());
      }
      serverErrorCount += response.getServerErrorCount();
      counter.addAll(response.getResults());
    }
    try {
      var offline = searchMetricsOffline(tenantId, searchMetricsRequest, cutoffWindow);
      counter.addAll(offline);
    } catch (Exception e) {
      log.error("Could search offline metrics due to {}", ExceptionUtils.debugFriendlyMsg(e));
      serverErrorCount += 1;
    }
    // batch the 400s,
    return SearchMetricsResponse.builder()
        .clientErrors(errors)
        .serverErrorCount(serverErrorCount)
        .results(counter.getPaths())
        .build();
  }

  private List<MetricsPathSpecifier> searchMetricsOffline(
      String tenantId, SearchMetricsRequest request, long cutoffWindow)
      throws StreamReadingException, IOException, ExecutionException {
    var clipped =
        IntervalUtils.clipBefore(request.getStartTime(), request.getEndTime(), cutoffWindow);
    if (clipped.isEmpty()) {
      return Collections.emptyList();
    }
    var hrStart = clipped.get().start() / 1000 / 3600;
    var hrEnd = clipped.get().end() / 1000 / 3600;
    var counter = new MetricsPathCounter();
    for (var hr = hrStart; hr <= hrEnd; hr++) {
      var result = searchMetricsOfflineForHr(request.getPattern(), hr, tenantId);
      for (var r : result) {
        counter.add(r);
      }
    }
    return counter.getPaths();
  }

  protected List<MetricsPathSpecifier> searchMetricsOfflineForHr(
      String pattern, long hr, String tenantId)
      throws StreamReadingException, IOException, ExecutionException {
    var prefix = metadataCache.listPaths(hr, tenantId);
    var matching = MetricsSearcher.searchMatchingMetrics(tenantId, prefix, pattern);
    return matching.stream()
        .map(
            m -> {
              return MetricsPathSpecifier.builder().name(m.name()).tags(m.tags()).build();
            })
        .toList();
  }
}
