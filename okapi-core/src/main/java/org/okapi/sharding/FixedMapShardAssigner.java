/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.sharding;

import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FixedMapShardAssigner<Id> implements ShardAssigner<Id> {

  Map<Long, Integer> shardMap;
  Map<Id, Integer> streamMap;

  @Override
  public int getShardForMetricsBlock(long blk) {
    return this.shardMap.get(blk);
  }

  @Override
  public int getShardForStream(Id streamId, long blk) {
    return streamMap.get(streamId);
  }
}
