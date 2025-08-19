package org.okapi.metrics.service.web;

import static org.okapi.validation.OkapiChecks.checkArgument;

import org.okapi.clock.Clock;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.MetricsPathParser;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssignerFactory;
import org.okapi.metrics.query.QueryRecords;
import org.okapi.metrics.rollup.RollupQueryProcessor;
import org.okapi.metrics.search.MetricsSearcher;
import com.okapi.rest.metrics.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class QueryProcessor {
  public static final long _1_HR = Duration.of(1, ChronoUnit.HOURS).toMillis();
  public static final long _24_HR = 24 * _1_HR;
  @Autowired ShardMap shardMap;
  @Autowired Clock clock;
  @Autowired ShardsAndSeriesAssignerFactory shardsAndSeriesAssignerFactory;
  @Autowired RollupQueryProcessor rollupQueryProcessor;
  @Autowired ServiceRegistry serviceRegistry;

  public GetMetricsResponse getMetricsResponse(GetMetricsRequestInternal request) throws Exception {
    var nodes = serviceRegistry.listActiveNodes();
    if (nodes.isEmpty()) {
      throw new BadRequestException("Cluster is scaling up, shard position unknown.");
    }

    var assigner = shardsAndSeriesAssignerFactory.makeAssigner(shardMap.getNsh(), nodes.get());
    var path = MetricPaths.convertToPath(request);
    var shard = assigner.getShard(path);
    var series = shardMap.get(shard);
    var result =
        rollupQueryProcessor.scan(
            series,
            new QueryRecords.Slice(
                path,
                request.getStart(),
                request.getEnd(),
                request.getResolution(),
                request.getAggregation()));
    return GetMetricsResponse.builder()
        .name(request.getMetricName())
        .resolution(request.getResolution())
        .aggregation(request.getAggregation())
        .tenant(request.getTenantId())
        .tags(request.getTags())
        .values(result.values())
        .times(result.timestamps())
        .build();
  }

  public SearchMetricsResponse searchMetricsResponse(
      SearchMetricsRequestInternal searchMetricsRequest) throws BadRequestException {
    checkArgument(searchMetricsRequest.getStartTime() > 0, BadRequestException::new);
    checkArgument(searchMetricsRequest.getEndTime() > 0, BadRequestException::new);
    var nShards = shardMap.getNsh();
    var candidates = new HashSet<String>();

    for (int shard = 0; shard < nShards; shard++) {
      var series = shardMap.get(shard);
      var keys = series.getKeys();
      for (var key : keys) {
        var parsed = MetricsPathParser.parseHashKey(key);
        if (parsed.isEmpty()) continue;
        if (!parsed.get().tenantId().equals(searchMetricsRequest.getTenantId())) continue;
        if (!parsed.get().resolution().equals("s")) {
          continue;
        }
        var secondsBucket = parsed.get().value();
        var millis = secondsBucket * 1000;
        if (millis > searchMetricsRequest.getEndTime()
            || millis < searchMetricsRequest.getStartTime()) continue;
        var metricPath =
            MetricPaths.convertToPath(
                parsed.get().tenantId(), parsed.get().name(), parsed.get().tags());
        candidates.add(metricPath);
      }
    }

    var matching =
        new HashSet<>(
            MetricsSearcher.searchMatchingMetrics(
                searchMetricsRequest.getTenantId(), candidates, searchMetricsRequest.getPattern()));
    var results =
        matching.stream()
            .map(s -> MetricsPathSpecifier.builder().name(s.name()).tags(s.tags()).build())
            .collect(Collectors.toSet());
    return SearchMetricsResponse.builder().results(new ArrayList<>(results)).build();
  }
}
