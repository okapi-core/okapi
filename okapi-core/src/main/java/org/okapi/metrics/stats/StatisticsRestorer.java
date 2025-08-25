package org.okapi.metrics.stats;

public interface StatisticsRestorer<T> {
    T deserialize(byte[] bytes);
}
