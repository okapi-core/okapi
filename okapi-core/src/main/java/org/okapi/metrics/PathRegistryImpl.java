package org.okapi.metrics;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class PathRegistryImpl implements PathRegistry {
  Path hourlyCheckpointRoot;
  Path shardPackageRoot;
  Path parquetRoot;
  Path shardAssetsRoot;

  ReadWriteLock readWriteLock;

  public PathRegistryImpl(
      Path hourlyCheckpointRoot,
      Path shardPackageRoot,
      Path parquetRoot,
      Path shardAssetsRoot,
      ReadWriteLock readWriteLock) {
    /** Ignore the passed lock its just for show */
    this.readWriteLock = new ReentrantReadWriteLock();
    this.hourlyCheckpointRoot = checkNotNull(hourlyCheckpointRoot);
    this.shardPackageRoot = checkNotNull(shardPackageRoot);
    this.parquetRoot = checkNotNull(parquetRoot);
    this.shardAssetsRoot = checkNotNull(shardAssetsRoot);
    Lists.newArrayList(hourlyCheckpointRoot, shardPackageRoot, parquetRoot, shardAssetsRoot)
        .forEach(
            dir -> {
              try {
                createDir(dir);
              } catch (IOException ioe) {
                log.info("Directory {} already exists, moving on.", dir);
              }
            });
  }

  public void createDir(Path dir) throws IOException {
    checkNotNull(dir);
    if (Files.exists(dir)) return;
    readWriteLock.writeLock().lock();
    try {
      if (!Files.exists(dir)) {
        Files.createDirectories(dir);
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
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
    return shardAssetsPath(shard).resolve("rocks");
  }

  @Override
  public Path shardAssetsPath(Integer shard) {
    var path = shardAssetsRoot.resolve(Integer.toString(shard));
    try {
      createDir(path);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    return path;
  }

  @Override
  public Path pathSetWal(Integer shard) {
    return shardAssetsPath(shard).resolve("pathSet.wal");
  }

  @Override
  public Path shardAssetsRoot() {
    return shardAssetsRoot;
  }
}
