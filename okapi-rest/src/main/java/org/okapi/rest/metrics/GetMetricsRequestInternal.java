package org.okapi.rest.metrics;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;

@AllArgsConstructor
@Builder
@Getter
public class GetMetricsRequestInternal {
    @NotNull(message = "Unknown tenant.") @Getter String tenantId;
    @NotNull(message = "Metrics name must be supplied.") @Getter String metricName;
    @NotNull(message = "Tags must be supplied") @Getter Map<String, String> tags;
    @NotNull(message = "start time is required.") long start;
    @NotNull(message = "End time is required.") long end;
    @NotNull(message = "resolution is required.")
    RES_TYPE resolution;
    @NotNull(message = "aggregation is required.")
    AGG_TYPE aggregation;
}
