/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.metrics.query;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
public class GetMetricsRequest {
  @NotNull(message = "Service name must be supplied.")
  String svc;

  @NotNull(message = "Metrics name must be supplied.")
  @Getter
  String metric;

  @NotNull(message = "Tags must be supplied")
  @Getter
  Map<String, String> tags;

  @NotNull(message = "start time is required.")
  long start;

  @NotNull(message = "End time is required.")
  long end;

  METRIC_TYPE metricType;
  GaugeQueryConfig gaugeQueryConfig;
  HistoQueryConfig histoQueryConfig;
  GetSumsQueryConfig sumsQueryConfig;
}
