package org.okapi.metrics.persistence;

import org.okapi.metrics.stats.UpdatableStatistics;

public interface PersistentStore  {
    UpdatableStatistics get(String key);
    void put(String key, UpdatableStatistics statistics);
}
