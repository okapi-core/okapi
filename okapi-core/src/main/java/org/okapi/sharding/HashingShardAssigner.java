/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.sharding;

import com.google.common.hash.Hashing;

public class HashingShardAssigner<Id> implements ShardAssigner<Id> {
  private final Integer totalShards;

  public HashingShardAssigner(Integer totalShards) {
    this.totalShards = totalShards;
  }

  public int getShardForMetricsBlock(long blk) {
    var hashFn = Hashing.murmur3_32_fixed();
    var hash = hashFn.newHasher().putLong(blk).hash();
    return hash.asInt() % totalShards;
  }

  public int getShardForStream(Id streamId, long blk) {
    var hashFn = Hashing.murmur3_32_fixed();
    var hash = hashFn.newHasher().putInt(streamId.hashCode()).hash();
    return hash.asInt() % totalShards;
  }
}
