/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import org.okapi.metrics.query.PayloadSplitter;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.sharding.ShardAssigner;

public class MetricsGrouperImpl implements MetricsGrouper {
  PayloadSplitter splitter;
  ShardAssigner<String> shardAssigner;
  MetricsStreamIdFactory streamIdFactory;

  public MetricsGrouperImpl(
      PayloadSplitter splitter,
      ShardAssigner<String> shardAssigner,
      MetricsStreamIdFactory streamIdFactory) {
    this.splitter = splitter;
    this.streamIdFactory = streamIdFactory;
    this.shardAssigner = shardAssigner;
  }

  @Override
  public Multimap<Integer, ExportMetricsRequest> groupByShard(
      List<ExportMetricsRequest> exportMetricsRequests) {
    var byShard = ArrayListMultimap.<Integer, ExportMetricsRequest>create();
    for (var req : exportMetricsRequests) {
      var split = splitter.splitPayloadByBlocks(req);
      for (var part : split.keys()) {
        var shard = shardAssigner.getShardForMetricsBlock(part);
        byShard.putAll(shard, split.get(part));
      }
    }
    return byShard;
  }
}
