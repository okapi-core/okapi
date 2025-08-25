package org.okapi.metrics;

import org.okapi.metrics.stats.Statistics;

public interface Merger<T extends Statistics> {

  Statistics merge(T A, T B);
}
