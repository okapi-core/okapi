package org.okapi.metrics.api;

import lombok.RequiredArgsConstructor;
import org.okapi.metrics.ch.ChMetricsQueryProcessor;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.search.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ChMetricsQueryController {
  private final ChMetricsQueryProcessor chMetricsQueryProcessor;

  @PostMapping("/metrics/query")
  public GetMetricsResponse getMetricsResponse(@RequestBody GetMetricsRequest getMetricsRequest) {
    return chMetricsQueryProcessor.getMetricsResponse(getMetricsRequest);
  }

  @PostMapping("/metrics/svc/hints")
  public GetMetricsHintsResponse getSvcHints(@RequestBody GetSvcHintsRequest getSvcHintsRequest) {
    return chMetricsQueryProcessor.getSvcHints(getSvcHintsRequest);
  }

  @PostMapping("/metrics/name/hints")
  public GetMetricsHintsResponse getMetricSuggestions(
      @RequestBody GetMetricNameHints hintsRequest) {
    return chMetricsQueryProcessor.getMetricHints(hintsRequest);
  }

  @PostMapping("/metrics/tag/hints")
  public GetMetricsHintsResponse getTagHints(@RequestBody GetTagHintsRequest request) {
    return chMetricsQueryProcessor.getTagHints(request);
  }

  @PostMapping("/metrics/tag-value/hints")
  public GetMetricsHintsResponse getTagValueHints(@RequestBody GetTagValueHintsRequest request) {
    return chMetricsQueryProcessor.getTagValueHints(request);
  }
}
