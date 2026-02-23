package org.okapi.metrics.service;

import java.util.HashMap;
import java.util.Map;

public class MetricsStreamIdFactory {
  public Map<Integer, MetricsStreamIdentifier> metricsStreamCache = new HashMap<>();

  public MetricsStreamIdentifier ofShard(int shard) {
    if (!metricsStreamCache.containsKey(shard)) {
      metricsStreamCache.put(shard, new MetricsStreamIdentifier(Integer.toString(shard)));
    }
    return metricsStreamCache.get(shard);
  }
}
