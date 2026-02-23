/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.abstractio;

import java.util.HashMap;
import java.util.Map;
import org.okapi.CommonConfig;

public class ShardToStringFlyweights {
  Map<Integer, String> shardToString;

  public ShardToStringFlyweights() {
    this.shardToString = new HashMap<>();
    for (int i = 0; i < CommonConfig.N_SHARDS; i++) {
      this.shardToString.put(i, Integer.toString(i));
    }
  }

  public String toString(int shard) {
    return this.shardToString.get(shard);
  }
}
