package org.okapi.metrics.stats;

public class RolledupStatsRestorer implements StatisticsRestorer<Statistics> {
  @Override
  public Statistics deserialize(byte[] bytes) {
    return RolledUpStatistics.deserialize(bytes, new KllSketchRestorer());
  }
}
