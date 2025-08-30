package org.okapi.promql.eval.ts;

import org.okapi.Statistics;

public interface StatisticsMerger {
  Statistics merge(Statistics a, Statistics b);
}
