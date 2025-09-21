package org.okapi.rest.metrics;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.TreeMap;
import lombok.*;
import org.okapi.rest.metrics.payloads.Gauge;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.Sum;

@Builder(toBuilder = true)
@NoArgsConstructor
@Getter
public class ExportMetricsRequest {
  @NotNull(message = "Unknown tenant.")
  @Setter
  String tenantId;

  @NotNull(message = "Metrics name must be supplied.")
  @Setter
  String metricName;

  @NotNull(message = "Tags must be supplied")
  Map<String, String> tags;

  @NotNull(message = "Metric type is required")
  @Setter
  MetricType type;

  @Nullable Gauge gauge;
  @Nullable Histo histo;
  @Nullable Sum sum;

  // Ensure tags are always sorted when set via setter (e.g., JSON deserialization)
  public void setTags(Map<String, String> tags) {
    this.tags = (tags == null) ? null : new TreeMap<>(tags);
  }

  // Ensure compatibility with existing constructor usage while sorting tags
  public ExportMetricsRequest(
      String tenantId,
      String metricName,
      Map<String, String> tags,
      MetricType type,
      @Nullable Gauge gauge,
      @Nullable Histo histo,
      @Nullable Sum sum) {
    this.tenantId = tenantId;
    this.metricName = metricName;
    this.tags = (tags == null) ? null : new TreeMap<>(tags);
    this.type = type;
    this.gauge = gauge;
    this.histo = histo;
    this.sum = sum;
  }

  // Customize Lombok builder to always sort tags
  public static class ExportMetricsRequestBuilder {
    public ExportMetricsRequestBuilder tags(Map<String, String> tags) {
      this.tags = (tags == null) ? null : new TreeMap<>(tags);
      return this;
    }
  }
}
