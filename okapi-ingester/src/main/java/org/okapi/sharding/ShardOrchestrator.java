/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.sharding;

public interface ShardOrchestrator {
  void startMove(int shardId) throws InterruptedException;

  void completeMove(int shardId) throws InterruptedException;
}
