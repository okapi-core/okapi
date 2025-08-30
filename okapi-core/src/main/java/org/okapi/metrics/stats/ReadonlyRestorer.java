package org.okapi.metrics.stats;

import org.okapi.Statistics;

public class ReadonlyRestorer implements StatisticsRestorer<Statistics> {
  @Override
  public Statistics deserialize(byte[] bytes) {
    return RolledUpStatistics.deserialize(bytes, new KllSketchRestorer());
  }
}
