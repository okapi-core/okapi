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
@Builder
@Getter
public class ChHistoSample {
  public enum HISTO_TYPE {
    DELTA,
    CUMULATIVE
  }

  String resource;
  String metric;
  Map<String, String> tags;
  HISTO_TYPE histoType;
  float sum;
  Long count;
  long tsStart;
  long tsEnd;
  float min;
  float max;
  float[] buckets;
  int[] counts;
}
