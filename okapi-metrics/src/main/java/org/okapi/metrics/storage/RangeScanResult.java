/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage;

import java.util.Collections;
import java.util.List;
import org.okapi.metrics.storage.snapshots.TimeSeriesSnapshot;

public class RangeScanResult {
  private List<TimeSeriesSnapshot> snapshots;

  public RangeScanResult(List<TimeSeriesSnapshot> snapshots) {
    this.snapshots = Collections.unmodifiableList(snapshots);
  }

  public TimeSeriesSnapshot get(int i) {
    return snapshots.get(i);
  }
}
