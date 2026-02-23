/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.sharding.registry;

import org.okapi.sharding.ShardRegistry;
import org.okapi.zk.NamespacedZkClient;

public class MetricsShardRegistry extends ShardRegistry {
  public MetricsShardRegistry(NamespacedZkClient namespacedZkClient) {
    super(namespacedZkClient);
  }
}
