/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.primitives.ReadOnlySketch;

public class SketchToResConverter {

  public static float getGaugeResult(ReadOnlySketch sketch, AGG_TYPE aggType) {
    return switch (aggType) {
      case AVG -> sketch.getMean();
      case SUM -> sketch.getMean() * sketch.getCount();
      case MIN -> sketch.getQuantile(0.0);
      case MAX -> sketch.getQuantile(1.0);
      case P50 -> sketch.getQuantile(0.5);
      case P90 -> sketch.getQuantile(0.9);
      case P95 -> sketch.getQuantile(0.95);
      case P99 -> sketch.getQuantile(0.99);
      default -> throw new IllegalArgumentException("Unsupported AGG_TYPE: " + aggType);
    };
  }
}
