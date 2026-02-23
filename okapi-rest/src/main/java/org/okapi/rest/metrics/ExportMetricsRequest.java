/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.metrics;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.TreeMap;
import lombok.*;
import org.okapi.rest.metrics.payloads.Gauge;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.Sum;

@NoArgsConstructor
@Getter
@ToString
public class ExportMetricsRequest {
  @Setter String resource;

  @NotNull(message = "Metrics name must be supplied.")
  @Setter
  String metricName;

  @NotNull(message = "Tags must be supplied")
  Map<String, String> tags;

  @NotNull(message = "Metric type is required")
  @Setter
  MetricType type;

  Gauge gauge;
  Histo histo;
  Sum sum;

  @Builder(toBuilder = true)
  public ExportMetricsRequest(
      String resource,
      String metricName,
      @Singular Map<String, String> tags,
      MetricType type,
      Gauge gauge,
      Histo histo,
      Sum sum) {
    this.resource = resource;
    this.metricName = metricName;
    this.tags = (tags == null) ? null : new TreeMap<>(tags);
    this.type = type;
    this.gauge = gauge;
    this.histo = histo;
    this.sum = sum;
  }

  public ExportMetricsRequest(
      String metricName,
      Map<String, String> tags,
      MetricType type,
      Gauge gauge,
      Histo histo,
      Sum sum) {
    this.metricName = metricName;
    this.tags = (tags == null) ? null : new TreeMap<>(tags);
    this.type = type;
    this.gauge = gauge;
    this.histo = histo;
    this.sum = sum;
  }

  // Ensure tags are always sorted when set via setter (e.g., JSON deserialization)
  public void setTags(Map<String, String> tags) {
    this.tags = (tags == null) ? null : new TreeMap<>(tags);
  }
}
