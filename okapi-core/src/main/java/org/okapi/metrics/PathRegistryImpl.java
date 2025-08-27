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
  Path hourlyCheckpointRoot;
  Path shardPackageRoot;
  Path parquetRoot;
  Path shardAssetsRoot;

  public PathRegistryImpl(Path hourlyCheckpointRoot, Path shardPackageRoot,
                          Path parquetRoot, Path shardAssetsRoot) {
    Lists.newArrayList(hourlyCheckpointRoot, shardPackageRoot, parquetRoot, shardAssetsRoot)
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
    this.hourlyCheckpointRoot = checkNotNull(hourlyCheckpointRoot);
    this.shardPackageRoot = checkNotNull(shardPackageRoot);
    this.parquetRoot = checkNotNull(parquetRoot);
    this.shardAssetsRoot = checkNotNull(shardAssetsRoot);
  }

  @Override
  public Path hourlyCheckpointPath(long hr, String tenantId) throws IOException {
    var path = hourlyCheckpointRoot.resolve(tenantId + "-hr" + ".ckpt");
    Files.createDirectories(path.getParent());
    return path;
  }

  @Override
  public Path shardPackagePath(int shard) throws IOException {
    return shardPackageRoot.resolve(shard + ".shard.ckpt");
  }

  @Override
  public Path parquetPath(long hr, String tenantId) {
    return parquetRoot.resolve(tenantId + "." + hr + ".parquet");
  }

  @Override
  public Path rocksPath(Integer shard) {
    return shardAssetsRoot.resolve(Integer.toString(shard)).resolve("rocks");
  }

  @Override
  public Path pathSetWal(Integer shard) {
    return shardAssetsRoot.resolve(Integer.toString(shard)).resolve("pathSet.wal");
  }
}
