package org.okapi.rest.metrics.search;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchMetricsRequest {
  String team;
  String pattern;
  long startTime;
  long endTime;
}
