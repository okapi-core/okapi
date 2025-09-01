package org.okapi.rest.metrics;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchMetricsRequestInternal {
    // use the search term to be tenantId:pattern
    String pattern;
    String tenantId;
    long startTime;
    long endTime;
}
