package org.okapi.logs.service;

import java.io.IOException;
import lombok.AllArgsConstructor;
import org.okapi.wal.consumer.WalConsumerController;
import org.okapi.wal.io.IllegalWalEntryException;

@AllArgsConstructor
public class LogsConsumerDriver {
  LogsShardWalConsumer logsShardWalConsumer;
  WalConsumerController controller;

  public void onTick() {
    if (!controller.canConsume()) {
      controller.stopAcked();
    } else {
      controller.startAcked();
    }

    try {
      logsShardWalConsumer.consumeRecords();
    } catch (IOException | IllegalWalEntryException e) {
      throw new RuntimeException(e);
    }
  }
}
