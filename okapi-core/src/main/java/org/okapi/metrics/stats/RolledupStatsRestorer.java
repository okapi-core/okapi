package org.okapi.metrics.stats;

public class RolledupStatsRestorer implements StatisticsRestorer<Statistics>{
    @Override
    public Statistics deserialize(byte[] bytes, QuantileRestorer restorer) {
        return RolledUpStatistics.deserialize(bytes, restorer);
    }
}
