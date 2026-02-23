/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.stats;

import java.util.function.Function;
import org.okapi.metrics.rollup.RollupSeries;

public class RollupSeriesFn implements Function<Integer, RollupSeries<UpdatableStatistics>> {
  @Override
  public RollupSeries<UpdatableStatistics> apply(Integer integer) {
    var statsSupplier = new KllStatSupplier();
    var series = new RollupSeries<>(statsSupplier, integer);
    return series;
  }
}
