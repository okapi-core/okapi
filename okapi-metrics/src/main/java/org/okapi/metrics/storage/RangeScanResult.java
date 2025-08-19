package org.okapi.metrics.storage;

import org.okapi.metrics.storage.snapshots.TimeSeriesSnapshot;
import java.util.Collections;
import java.util.List;

public class RangeScanResult {
  private List<TimeSeriesSnapshot> snapshots;

  public RangeScanResult(List<TimeSeriesSnapshot> snapshots) {
    this.snapshots = Collections.unmodifiableList(snapshots);
  }

  public TimeSeriesSnapshot get(int i){
      return snapshots.get(i);
  }
}
