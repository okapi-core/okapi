/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.metrics.query;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@ToString
public class GetMetricsRequest {
  @JsonPropertyDescription("Name of the metric to query (e.g. \"http.server.duration\").")
  @NotNull(message = "Metrics name must be supplied.")
  @Getter
  String metric;

  @JsonPropertyDescription(
      "Exact key-value label pairs the metric series must match. All entries must be present on the series (subset match).")
  @NotNull(message = "Tags must be supplied")
  @Getter
  Map<String, String> tags;

  @JsonPropertyDescription("Query window start time, in milliseconds since Unix epoch (inclusive).")
  @NotNull(message = "start time is required.")
  long start;

  @JsonPropertyDescription("Query window end time, in milliseconds since Unix epoch (exclusive).")
  @NotNull(message = "End time is required.")
  long end;

  @JsonPropertyDescription(
"""
The type of metric to query. Acceptable values are:  GAUGE, HISTO, or SUM.
Only the type that is set will be queried.
e.g. if metricType is set to GAUGE, the metric is cpu_usage and tags is {"container": "container-abc"},
then query will look for a metric cpu_usage with tags container=container-abc and of type GAUGE.
`metric`, `tags` and `metricType` uniquely identify a metric.
  """)
          @NotNull(message = "metricType should be specified.")
  METRIC_TYPE metricType;

  @JsonPropertyDescription(
"""
Configuration for gauge queries: time bucket resolution and aggregation function. Required when metricType is GAUGE.
Ignored for any other type.
  """)
  GaugeQueryConfig gaugeQueryConfig;

  @JsonPropertyDescription(
"""
Configuration for histogram queries: temporality mode. Required when metricType is HISTO.
Ignored for any other type.
  """)
  HistoQueryConfig histoQueryConfig;

  @JsonPropertyDescription(
"""
Configuration for sum queries: temporality mode. Required when metricType is SUM.
Ignored for any other type.
""")
  GetSumsQueryConfig sumsQueryConfig;
}
