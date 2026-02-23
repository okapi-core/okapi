/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.query;

import com.google.gson.Gson;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.okapi.parallel.ParallelExecutor;
import org.okapi.rest.metrics.query.GetMetricsBatchResponse;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.search.GetMetricNameHints;
import org.okapi.rest.search.GetMetricsHintsResponse;
import org.okapi.rest.search.GetSvcHintsRequest;
import org.okapi.rest.search.GetTagHintsRequest;
import org.okapi.rest.search.GetTagValueHintsRequest;
import org.okapi.web.auth.AccessManager;
import org.okapi.web.auth.TokenManager;
import org.okapi.web.dtos.dashboards.MultiQueryPanelWDto;
import org.okapi.web.service.Configs;
import org.okapi.web.service.client.IngesterClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MetricsQueryService {

  OkHttpClient client;
  TokenManager tokenManager;
  AccessManager accessManager;
  ProxyResponseTranslator translator;
  String clusterEndpoint;
  ParallelExecutor parallelExecutor;
  long timeoutMillis;
  Gson gson = new Gson();
  IngesterClient ingesterClient;

  public MetricsQueryService(
      OkHttpClient client,
      TokenManager tokenManager,
      AccessManager accessManager,
      ProxyResponseTranslator proxyResponseTranslator,
      @Value(Configs.CLUSTER_EP) String clusterEndpoint,
      @Value(Configs.CONCURRENT_QUERY_THREADS) int queryThreads,
      @Value(Configs.CONCURRENCY_QUERY_LIM) int queryLim,
      @Value(Configs.QUERY_TIMEOUT) long timeoutMillis,
      IngesterClient ingesterClient) {
    this.client = client;
    this.tokenManager = tokenManager;
    this.accessManager = accessManager;
    this.translator = proxyResponseTranslator;
    this.clusterEndpoint = clusterEndpoint;
    this.parallelExecutor = new ParallelExecutor(queryLim, queryThreads);
    this.timeoutMillis = timeoutMillis;
    this.ingesterClient = ingesterClient;
  }

  public GetMetricsResponse queryMetrics(String token, GetMetricsRequest getMetricsRequest) {
    var userId = tokenManager.getUserId(token);
    var orgId = tokenManager.getOrgId(token);
    accessManager.checkUserIsOrgMember(new AccessManager.AuthContext(userId, orgId));
    return ingesterClient.query(getMetricsRequest);
  }

  public GetMetricsHintsResponse getSvcHints(String token, GetSvcHintsRequest request) {
    validateAccess(token);
    return ingesterClient.getSvcHints(request);
  }

  public GetMetricsHintsResponse getMetricHints(String token, GetMetricNameHints request) {
    validateAccess(token);
    return ingesterClient.getMetricHints(request);
  }

  public GetMetricsHintsResponse getTagHints(String token, GetTagHintsRequest request) {
    validateAccess(token);
    return ingesterClient.getTagHints(request);
  }

  public GetMetricsHintsResponse getTagValueHints(String token, GetTagValueHintsRequest request) {
    validateAccess(token);
    return ingesterClient.getTagValueHints(request);
  }

  private void validateAccess(String token) {
    var userId = tokenManager.getUserId(token);
    var orgId = tokenManager.getOrgId(token);
    accessManager.checkUserIsOrgMember(new AccessManager.AuthContext(userId, orgId));
  }

  public GetMetricsBatchResponse queryMetrics(String token, MultiQueryPanelWDto multiQueryPanelWDto)
      throws MalformedQueryException {
    validateAccess(token);
    var fetchers = new ArrayList<Supplier<GetMetricsResponse>>();
    for (var rawQuery : multiQueryPanelWDto.getQueries()) {
      var substituted =
          VarsPreprocessor.substituteVars(
              rawQuery.getQuery(), multiQueryPanelWDto.getVarsContext().getVarValues());
      var parsed = gson.fromJson(substituted, GetMetricsRequest.class);
      var constrained =
          ConstraintEnforcer.applyTimeConstraint(parsed, multiQueryPanelWDto.getTimeConstraint());
      fetchers.add(() -> ingesterClient.query(constrained));
    }
    var results =
        this.parallelExecutor.submit(fetchers, Duration.of(this.timeoutMillis, ChronoUnit.MILLIS));
    return new GetMetricsBatchResponse(results);
  }

  @PreDestroy
  public void tearDown() throws IOException {
    this.parallelExecutor.close();
  }
}
