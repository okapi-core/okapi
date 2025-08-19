package org.okapi.metrics.common.sharding;


public interface ShardsAndSeriesAssigner {
  String getNode(int shardId);

  int getShard(String metricPath);
}
