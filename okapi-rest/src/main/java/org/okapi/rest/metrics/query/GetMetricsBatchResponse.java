package org.okapi.rest.metrics.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class GetMetricsBatchResponse {
    List<GetMetricsResponse> responses;
}
