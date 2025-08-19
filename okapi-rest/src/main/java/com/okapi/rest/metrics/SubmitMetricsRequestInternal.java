package com.okapi.rest.metrics;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.*;

@Builder
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class SubmitMetricsRequestInternal {
  @NotNull(message = "Unknown tenant.") @Getter String tenantId;
  @NotNull(message = "Metrics name must be supplied.") @Getter String metricName;
  @NotNull(message = "Tags must be supplied") @Getter Map<String, String> tags;
  @NotNull(message = "Values must be supplied") @Getter float[] values;
  @NotNull(message = "Times must be supplied") @Getter long[] ts;
}
