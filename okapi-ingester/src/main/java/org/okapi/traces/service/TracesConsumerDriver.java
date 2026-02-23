package org.okapi.traces.service;

import java.io.IOException;
import lombok.AllArgsConstructor;
import org.okapi.io.StreamReadingException;
import org.okapi.wal.consumer.WalConsumerController;
import org.okapi.wal.io.IllegalWalEntryException;

@AllArgsConstructor
public class TracesConsumerDriver {
  TracesShardWalConsumer tracesShardWalConsumer;
  WalConsumerController controller;

  public void onTick() {
    if (!controller.canConsume()) {
      controller.stopAcked();
    } else {
      controller.startAcked();
    }

    try {
      tracesShardWalConsumer.consumeRecords();
    } catch (IOException | IllegalWalEntryException | StreamReadingException e) {
      throw new RuntimeException(e);
    }
  }
}
