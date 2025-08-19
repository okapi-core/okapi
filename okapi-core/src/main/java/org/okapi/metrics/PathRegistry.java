package org.okapi.metrics;

import java.io.IOException;
import java.nio.file.Path;

public interface PathRegistry {
  Path checkpointUploaderRoot(int shard, long hr, String tenantId) throws IOException;
  Path shardCheckpointPath(int shard) throws  IOException;
  Path parquetPath(long hr, String tenantId);
}
