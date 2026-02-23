/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.sharding;

public interface ShardAssigner<Id> {
  int getShardForMetricsBlock(long blk);

  int getShardForStream(Id streamId, long blk);
}
