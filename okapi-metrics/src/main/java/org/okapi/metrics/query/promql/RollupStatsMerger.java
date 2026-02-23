/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query.promql;

import lombok.AllArgsConstructor;
import org.okapi.Statistics;
import org.okapi.metrics.Merger;
import org.okapi.metrics.stats.RolledUpStatistics;
import org.okapi.metrics.stats.UpdatableStatistics;
import org.okapi.promql.eval.ts.StatisticsMerger;

@AllArgsConstructor
public class RollupStatsMerger implements StatisticsMerger {

  Merger<UpdatableStatistics> merger;

  @Override
  public Statistics merge(Statistics a, Statistics b) {
    var castA = (RolledUpStatistics) a;
    var castB = (RolledUpStatistics) b;
    var merged = merger.merge(castA, castB);
    return merged;
  }
}
