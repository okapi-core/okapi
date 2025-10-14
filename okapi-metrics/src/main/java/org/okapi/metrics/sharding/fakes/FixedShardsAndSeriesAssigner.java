package org.okapi.metrics.sharding.fakes;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;

@Slf4j
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
    var node = shardToNodeMap.get(shardId);
    return node;
  }

  @Override
  public int getShard(String metricPath) {
    var shard = seriesToShardMap.get(metricPath);
    if (shard == null) {
      log.error("This method will crash, since shard for path {} is null.", shard);
    }
    return seriesToShardMap.get(metricPath);
  }
}
