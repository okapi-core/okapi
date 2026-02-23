package org.okapi.sharding.registry;

import org.okapi.sharding.ShardRegistry;
import org.okapi.zk.NamespacedZkClient;

public class LogsShardRegistry extends ShardRegistry {
    public LogsShardRegistry(NamespacedZkClient namespacedZkClient) {
        super(namespacedZkClient);
    }
}
