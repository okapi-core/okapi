package org.okapi.metrics;

import org.okapi.metrics.stats.UpdatableStatistics;

public interface Merger<T extends UpdatableStatistics> {

  UpdatableStatistics merge(T A, T B);
}
