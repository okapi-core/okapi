/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.sharding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.okapi.wal.consumer.WalConsumerControllers;
import org.okapi.zk.NamespacedZkClient;

@ExtendWith(MockitoExtension.class)
public class ShardMoveOrchestratorTests {
  @Mock ShardUploader uploader;
  @Mock NamespacedZkClient zkClient;

  ShardMoveOrchestrator shardMoveOrchestrator;
  ShardMoveOrchestrator.ShardMoveConfig moveConfig;
  WalConsumerControllers consumerControllers;

  @BeforeEach
  void setup() {
    moveConfig = new ShardMoveOrchestrator.ShardMoveConfig(1000L);
    consumerControllers = new WalConsumerControllers();
    shardMoveOrchestrator = new ShardMoveOrchestrator(uploader, consumerControllers, moveConfig);
  }

  @Test
  void testCompleteMove() throws InterruptedException {
    shardMoveOrchestrator.completeMove(1);
  }
}
