package org.okapi.rest.metrics;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.*;
import org.okapi.rest.metrics.payloads.Gauge;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.Sum;

@Builder(toBuilder = true)
@AllArgsConstructor
@Setter
@NoArgsConstructor
@Getter
public class ExportMetricsRequest {
  @NotNull(message = "Unknown tenant.")
  String tenantId;

  @NotNull(message = "Metrics name must be supplied.")
  String metricName;

  @NotNull(message = "Tags must be supplied")
  Map<String, String> tags;

  @NotNull(message = "Metric type is required")
  MetricType type;

  @Nullable Gauge gauge;
  @Nullable Histo histo;
  @Nullable Sum sum;
}
