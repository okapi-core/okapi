/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.zk;

import java.util.List;
import org.okapi.sharding.ShardMetadata;

public interface NamespacedZkClient {
  enum STATE {
    ASSIGNED,
    STEADY,
    MOVING
  };

  List<Integer> getMyShards();

  ShardMetadata getShardState(int shardId);
}
