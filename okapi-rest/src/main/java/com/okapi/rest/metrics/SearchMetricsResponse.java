package com.okapi.rest.metrics;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class SearchMetricsResponse {
    List<MetricsPathSpecifier> results;
    int serverErrorCount;
    List<String> clientErrors;
}
