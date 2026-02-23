package org.okapi.zk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.okapi.sharding.ShardMetadata;

@AllArgsConstructor
public class FakeZkClient implements NamespacedZkClient {
  List<Integer> assigned;
  Map<Integer, ShardMetadata> metadataMap;

  public FakeZkClient(List<Integer> assigned) {
    this(assigned, new HashMap<>());
  }

  public void update(int shard, ShardMetadata metadata) {
    ensureMetadata();
    this.metadataMap.put(shard, metadata);
  }

  public void updateAll(Map<Integer, ShardMetadata> metadata) {
    ensureMetadata();
    this.metadataMap.putAll(metadata);
  }

  public void updateAssigned(List<Integer> assigned) {
    this.assigned = assigned;
  }

  @Override
  public List<Integer> getMyShards() {
    return assigned;
  }

  @Override
  public ShardMetadata getShardState(int shardId) {
    return metadataMap.get(shardId);
  }

  private void ensureMetadata() {
    if (metadataMap == null) {
      metadataMap = new HashMap<>();
    }
  }
}
