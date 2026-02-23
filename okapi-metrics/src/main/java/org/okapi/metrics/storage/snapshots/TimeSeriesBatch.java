/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage.snapshots;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

public class TimeSeriesBatch {
  @Getter List<Double> vals;
  @Getter List<Long> ts;

  public TimeSeriesBatch(List<Long> ts, List<Double> vals) {
    // small batch decoding
    this.ts = Collections.unmodifiableList(ts);
    this.vals = Collections.unmodifiableList(vals);
  }
}
