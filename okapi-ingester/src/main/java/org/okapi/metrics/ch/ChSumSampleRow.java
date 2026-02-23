/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.ch;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ChSumSampleRow {
  String resource;
  String metricName;
  Map<String, String> tags;
  long tsStart;
  long tsEnd;
  long value;
  CH_SUM_TYPE sumType;
}
