/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.metrics.query;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class GetMetricsResponse {
  String resource;
  String metric;
  Map<String, String> tags;
  GetGaugeResponse gaugeResponse;
  GetHistogramResponse histogramResponse;
  GetSumsResponse sumsResponse;
}
