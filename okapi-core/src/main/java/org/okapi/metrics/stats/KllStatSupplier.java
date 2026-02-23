/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.stats;

import java.util.function.Supplier;
import org.apache.datasketches.kll.KllFloatsSketch;

public class KllStatSupplier implements Supplier<UpdatableStatistics> {
  public static KllFloatsSketch kllSketch() {
    return KllFloatsSketch.newHeapInstance(200);
  }

  @Override
  public UpdatableStatistics get() {
    return new RolledUpStatistics(kllSketch());
  }
}
