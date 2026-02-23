package org.okapi.sharding;

public interface ShardAssigner<Id> {
  int getShardForMetricsBlock(long blk);

  int getShardForStream(Id streamId, long blk);
}
