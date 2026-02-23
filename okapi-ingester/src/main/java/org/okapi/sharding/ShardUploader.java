package org.okapi.sharding;

import java.io.IOException;

public interface ShardUploader {
  void uploadShard(int shardId) throws IOException;
}
