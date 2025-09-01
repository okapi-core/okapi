package org.okapi.rest.metrics;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchMetricsRequest {
    // use the search term to be tenantId:pattern
    String team;
    String pattern;
    long startTime;
    long endTime;
}
