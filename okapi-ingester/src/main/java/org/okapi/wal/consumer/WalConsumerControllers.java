package org.okapi.wal.consumer;

import java.util.HashMap;
import java.util.Map;
import org.okapi.CommonConfig;

public class WalConsumerControllers {
  Map<Integer, WalConsumerController> perShardConsumerController = new HashMap<>();

  public WalConsumerControllers() {
    for (var shard = 0; shard < CommonConfig.N_SHARDS; shard++) {
      perShardConsumerController.put(shard, new WalConsumerController());
    }
  }

  public WalConsumerController getController(int shard) {
    return perShardConsumerController.get(shard);
  }
}
