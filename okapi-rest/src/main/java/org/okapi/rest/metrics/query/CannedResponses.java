package org.okapi.rest.metrics.query;

import java.util.Map;

public class CannedResponses {
  public static GetMetricsResponse noMetricsResponse(
      String resource, String metric, Map<String, String> tags) {
    return GetMetricsResponse.builder().resource(resource).metric(metric).tags(tags).build();
  }
}
