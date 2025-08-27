package org.okapi.metrics;

import java.io.IOException;
import java.nio.file.Path;

public interface PathRegistry {
  Path hourlyCheckpointPath(long hr, String tenantId) throws IOException;
  Path shardPackagePath(int shard) throws  IOException;
  Path parquetPath(long hr, String tenantId);
  Path rocksPath(Integer shard);
  Path shardAssetsPath(Integer shard);
  Path pathSetWal(Integer shard);
  Path shardAssetsRoot();
}
