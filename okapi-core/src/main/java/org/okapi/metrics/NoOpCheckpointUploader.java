package org.okapi.metrics;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class NoOpCheckpointUploader implements CheckpointUploaderDownloader {
  @Override
  public void uploadHourlyCheckpoint(String tenantId, Path path, long epochHourBucket, int shard)
      throws Exception {
    log.info("Doing nothing.");
  }

  @Override
  public void uploadShardCheckpoint(Path path, String opId, int shard) throws IOException {
    log.info("Doing nothing.");
  }

  @Override
  public void downloadShardCheckpoint(String opId, int shard, Path path) throws IOException {
    throw new IllegalStateException();
  }

  @Override
  public void uploadParquetDump(String tenantId, Path path, long epochHour) {
    log.info("Doing nothing.");
  }
}
