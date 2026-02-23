package org.okapi.sharding.registry;

import org.okapi.sharding.ShardRegistry;
import org.okapi.zk.NamespacedZkClient;

public class MetricsShardRegistry extends ShardRegistry {
    public MetricsShardRegistry(NamespacedZkClient namespacedZkClient) {
        super(namespacedZkClient);
    }
}
