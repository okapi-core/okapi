package com.okapi.rest.metrics;


import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Builder
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class SubmitMetricsRequest {
    @NotNull(message = "Metrics name must be supplied.") @Getter
    String metricName;
    @NotNull(message = "Tags must be supplied") @Getter
    Map<String, String> tags;
    @NotNull(message = "Values must be supplied") @Getter float[] values;
    @NotNull(message = "Times must be supplied") @Getter long[] ts;
}
