package org.okapi.sharding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.okapi.wal.consumer.WalConsumerControllers;

@Slf4j
@Builder
@AllArgsConstructor
public class ShardMoveOrchestrator implements ShardOrchestrator {

  @AllArgsConstructor
  @Getter
  public static class ShardMoveConfig {
    long walConsumerAckWaitMillis;
  }

  ShardUploader uploader;
  WalConsumerControllers consumerControllers;
  ShardMoveConfig shardMoveConfig;

  @Override
  public void startMove(int shard) {
    var controller = consumerControllers.getController(shard);
    controller.stop();
    try {
      controller.stopIsAcked(shardMoveConfig.walConsumerAckWaitMillis);
    } catch (InterruptedException e) {
      controller.start();
      log.error("Could not stop consumer.");
    }
  }

  @Override
  public void completeMove(int shard) {
    var controller = consumerControllers.getController(shard);
    controller.start();
    try {
      controller.startIsAcked(shardMoveConfig.walConsumerAckWaitMillis);
    } catch (InterruptedException e) {
      log.error("Could not start consumer.");
    }
  }
}
