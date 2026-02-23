package org.okapi.web.controller;

import lombok.AllArgsConstructor;
import org.okapi.headers.CookiesAndHeaders;
import org.okapi.rest.metrics.query.GetMetricsBatchResponse;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.search.GetMetricNameHints;
import org.okapi.rest.search.GetMetricsHintsResponse;
import org.okapi.rest.search.GetSvcHintsRequest;
import org.okapi.rest.search.GetTagHintsRequest;
import org.okapi.rest.search.GetTagValueHintsRequest;
import org.okapi.web.dtos.dashboards.MultiQueryPanelWDto;
import org.okapi.web.service.query.MalformedQueryException;
import org.okapi.web.service.query.MetricsQueryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/metrics")
@AllArgsConstructor
public class MetricsQueryController {

  MetricsQueryService metricsQueryService;

  @GetMapping("/query")
  public GetMetricsResponse getMetricsResponse(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestBody GetMetricsRequest request) {
    return metricsQueryService.queryMetrics(tempToken, request);
  }

  @PostMapping("/query/batch")
  public GetMetricsBatchResponse getMetricsBatchResponse(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestBody MultiQueryPanelWDto panelWDto)
      throws MalformedQueryException {
    return metricsQueryService.queryMetrics(tempToken, panelWDto);
  }

  @PostMapping("/svc/hints")
  public GetMetricsHintsResponse getSvcHints(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestBody GetSvcHintsRequest request) {
    return metricsQueryService.getSvcHints(tempToken, request);
  }

  @PostMapping("/name/hints")
  public GetMetricsHintsResponse getMetricHints(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestBody GetMetricNameHints request) {
    return metricsQueryService.getMetricHints(tempToken, request);
  }

  @PostMapping("/tag/hints")
  public GetMetricsHintsResponse getTagHints(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestBody GetTagHintsRequest request) {
    return metricsQueryService.getTagHints(tempToken, request);
  }

  @PostMapping("/tag-value/hints")
  public GetMetricsHintsResponse getTagValueHints(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestBody GetTagValueHintsRequest request) {
    return metricsQueryService.getTagValueHints(tempToken, request);
  }
}
