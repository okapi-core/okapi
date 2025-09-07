package org.okapi.rest.metrics;

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
