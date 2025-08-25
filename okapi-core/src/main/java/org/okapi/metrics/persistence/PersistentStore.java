package org.okapi.metrics.persistence;

import org.okapi.metrics.stats.Statistics;

public interface PersistentStore  {
    Statistics get(String key);
    void put(String key, Statistics statistics);
}
