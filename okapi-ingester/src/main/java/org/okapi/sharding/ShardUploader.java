/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.sharding;

import java.io.IOException;

public interface ShardUploader {
  void uploadShard(int shardId) throws IOException;
}
