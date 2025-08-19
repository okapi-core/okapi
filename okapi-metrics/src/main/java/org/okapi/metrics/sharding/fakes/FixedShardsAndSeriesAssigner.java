package org.okapi.metrics.sharding.fakes;

import lombok.NoArgsConstructor;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
public class FixedShardsAndSeriesAssigner implements ShardsAndSeriesAssigner {

  Map<String, Integer> seriesToShardMap = new HashMap<>();
  Map<Integer, String> shardToNodeMap = new HashMap<>();

  public FixedShardsAndSeriesAssigner assignShard(int shard, String id) {
    this.shardToNodeMap.put(shard, id);
    return this;
  }

  public FixedShardsAndSeriesAssigner assignShard(int st, int end, String id) {
    for (var i = st; i < end; i++) {
      shardToNodeMap.put(i, id);
    }
    return this;
  }

  public FixedShardsAndSeriesAssigner assignSeries(String series, int shard) {
    this.seriesToShardMap.put(series, shard);
    return this;
  }

  @Override
  public String getNode(int shardId) {
    return shardToNodeMap.get(shardId);
  }

  @Override
  public int getShard(String metricPath) {
    return seriesToShardMap.get(metricPath);
  }
}
