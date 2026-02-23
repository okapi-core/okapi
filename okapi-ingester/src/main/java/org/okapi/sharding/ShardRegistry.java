package org.okapi.sharding;

import java.util.List;
import lombok.AllArgsConstructor;
import org.okapi.zk.NamespacedZkClient;

@AllArgsConstructor
public class ShardRegistry {
  NamespacedZkClient namespacedZkClient;

  public ShardMetadata getShardState(int shardId) {
    return namespacedZkClient.getShardState(shardId);
  }

  public List<Integer> getAssigned() {
    return namespacedZkClient.getMyShards();
  }
}
