/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics;

import java.io.IOException;
import java.nio.file.Path;

public interface CheckpointUploaderDownloader {
  void uploadHourlyCheckpoint(String tenantId, Path path, long epochHourBucket) throws Exception;

  void uploadShardCheckpoint(Path path, String opId, int shard) throws IOException;

  void downloadShardCheckpoint(String opId, int shard, Path path) throws IOException;

  void uploadParquetDump(String tenantId, Path path, long epochHour) throws IOException;
}
