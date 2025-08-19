package org.okapi.metrics.common;

public interface PrefixSupplier {
    String getHourlyCheckpoint(String tenantId, long epochHour, int shard);
}
