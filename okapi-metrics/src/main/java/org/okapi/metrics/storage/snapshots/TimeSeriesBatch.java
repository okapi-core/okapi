package org.okapi.metrics.storage.snapshots;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

public class TimeSeriesBatch {
  @Getter List<Double> vals;
  @Getter List<Long> ts;

  public TimeSeriesBatch(List<Long> ts, List<Double> vals) {
    // small batch decoding
    this.ts = Collections.unmodifiableList(ts);
    this.vals = Collections.unmodifiableList(vals);
  }
}
