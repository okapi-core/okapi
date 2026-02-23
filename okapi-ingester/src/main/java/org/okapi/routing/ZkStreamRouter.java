/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.routing;

import org.okapi.sharding.ShardAssigner;
import org.okapi.sharding.ShardState;
import org.okapi.streams.StreamIdentifier;
import org.okapi.zk.NamespacedZkClient;

public class ZkStreamRouter implements StreamRouter<String> {
  NamespacedZkClient namespacedZkClient;
  ShardAssigner<String> shardAssigner;

  @Override
  public String getNodesForReading(StreamIdentifier<String> streamIdentifier, long blockNum) {
    var shard = shardAssigner.getShardForStream(streamIdentifier.getStreamId(), blockNum);
    var state = namespacedZkClient.getShardState(shard);
    if (state.getState() == ShardState.STEADY) {
      return state.getOwner();
    } else {
      return state.getTarget();
    }
  }
}
