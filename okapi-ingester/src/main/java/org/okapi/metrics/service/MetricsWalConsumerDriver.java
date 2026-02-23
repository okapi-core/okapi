/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.service;

import java.io.IOException;
import lombok.AllArgsConstructor;
import org.okapi.wal.consumer.WalConsumerController;
import org.okapi.wal.io.IllegalWalEntryException;

@AllArgsConstructor
public class MetricsWalConsumerDriver {
  MetricsShardWalConsumer metricsShardWalConsumer;
  WalConsumerController controller;

  public void onTick() {
    if (!controller.canConsume()) {
      controller.stopAcked();
    } else {
      controller.startAcked();
    }

    try {
      metricsShardWalConsumer.consumeRecords();
    } catch (IOException | IllegalWalEntryException e) {
      throw new RuntimeException(e);
    }
  }
}
