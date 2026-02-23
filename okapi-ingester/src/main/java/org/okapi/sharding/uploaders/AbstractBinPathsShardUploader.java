/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.sharding.uploaders;

import java.io.IOException;
import lombok.AllArgsConstructor;
import org.okapi.runtime.GenericS3Uploader;
import org.okapi.sharding.ShardAssigner;
import org.okapi.sharding.ShardUploader;

@AllArgsConstructor
public abstract class AbstractBinPathsShardUploader implements ShardUploader {
  ShardAssigner<String> shardAssigner;
  GenericS3Uploader<String> genericS3Uploader;

  @Override
  public void uploadShard(int shardId) throws IOException {
    genericS3Uploader.uploadFilesMatchingCondition(
        (fileInfo) ->
            shardAssigner.getShardForStream(fileInfo.stream(), fileInfo.blk()) == shardId);
  }
}
