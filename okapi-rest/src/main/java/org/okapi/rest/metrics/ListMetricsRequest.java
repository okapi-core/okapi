package org.okapi.rest.metrics;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Builder
@Value
@Getter
public class ListMetricsRequest {
    String tenantId;
    long start;
    long end;
}
