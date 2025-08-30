package org.okapi.metrics.stats;

public class WritableRestorer implements StatisticsRestorer<UpdatableStatistics> {
  @Override
  public UpdatableStatistics deserialize(byte[] bytes) {
    return RolledUpStatistics.deserialize(bytes, new KllSketchRestorer());
  }
}
