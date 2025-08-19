package org.okapi.metrics;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class PathRegistryImpl implements PathRegistry {
  Path checkpointUploaderRoot;
  Path shardCheckpointRoot;
  Path parquetRoot;

  public PathRegistryImpl(Path checkpointUploaderRoot, Path shardCheckpointRoot, Path parquetRoot) {
    Lists.newArrayList(checkpointUploaderRoot, shardCheckpointRoot, parquetRoot)
        .forEach(
            dir -> {
              try {
                Files.createDirectories(dir);
              } catch (FileAlreadyExistsException fileAlreadyExistsException) {
                log.info("Directory {} already exists, moving on.", dir);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    this.checkpointUploaderRoot = checkNotNull(checkpointUploaderRoot);
    this.shardCheckpointRoot = checkNotNull(shardCheckpointRoot);
    this.parquetRoot = checkNotNull(parquetRoot);
  }

  @Override
  public Path checkpointUploaderRoot(int shard, long hr, String tenantId) throws IOException {
    var path = checkpointUploaderRoot.resolve("" + shard).resolve(tenantId + "-hr" + ".ckpt");
    Files.createDirectories(path.getParent());
    return path;
  }

  @Override
  public Path shardCheckpointPath(int shard) throws IOException {
    return shardCheckpointRoot.resolve(shard + ".shard.ckpt");
  }

  @Override
  public Path parquetPath(long hr, String tenantId) {
    return parquetRoot.resolve(tenantId + "." + hr + ".parquet");
  }
}
