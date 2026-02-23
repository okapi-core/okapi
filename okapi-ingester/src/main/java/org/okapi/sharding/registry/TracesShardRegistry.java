package org.okapi.sharding.registry;

import org.okapi.sharding.ShardRegistry;
import org.okapi.zk.NamespacedZkClient;

public class TracesShardRegistry extends ShardRegistry {
  public TracesShardRegistry(NamespacedZkClient namespacedZkClient) {
    super(namespacedZkClient);
  }
}
