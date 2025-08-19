package org.okapi.metrics.common.sharding;

import org.okapi.consistenthashing.ConsistentHasher;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class ConsistentHashBasedAssigner implements ShardsAndSeriesAssigner {
  int nShards;
  List<String> nodes;

  @Override
  public String getNode(int shardId) {
    return ConsistentHasher.mapShard(shardId, nodes);
  }

  @Override
  public int getShard(String metricPath) {
    return ConsistentHasher.hash(metricPath, nShards);
  }
}
