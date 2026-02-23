package org.okapi.abstractio;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StreamIdFactory {
  ShardToStringFlyweights shardToStringFlyweights;

  public LogStreamIdentifier withStreamAndShard(int shard) {
    var streamId = shardToStringFlyweights.toString(shard);
    return LogStreamIdentifier.of(streamId);
  }
}
