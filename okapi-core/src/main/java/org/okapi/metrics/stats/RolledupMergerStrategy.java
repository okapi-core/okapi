/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.stats;

import org.okapi.metrics.Merger;

public class RolledupMergerStrategy implements Merger<UpdatableStatistics> {

  @Override
  public UpdatableStatistics merge(UpdatableStatistics A, UpdatableStatistics B) {
    var asRolledUpA = (RolledUpStatistics) A;
    var asRolledUpB = (RolledUpStatistics) B;
    var count = A.getCount() + B.getCount();
    var sum = A.getSum() + B.getSum();
    var kll = KllStatSupplier.kllSketch();
    kll.merge(asRolledUpA.getSketch());
    kll.merge(asRolledUpB.getSketch());
    return new RolledUpStatistics(sum, count, kll);
  }
}
