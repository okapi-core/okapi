/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.service;

import com.google.gson.Gson;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.okapi.CommonConfig;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.identity.MemberList;
import org.okapi.pages.MetricsBufferPool;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.ForwardedMetricsRequest;
import org.okapi.sharding.ShardRegistry;
import org.okapi.sharding.ShardState;
import org.okapi.wal.io.IllegalWalEntryException;

@RequiredArgsConstructor
public class MetricsShardWalConsumer {
  private final WalResourcesPerStream<Integer> walResourcesPerStream;
  private final MetricsBufferPool bufferPool;
  private final Integer batchSize;
  private final MetricsStreamIdFactory metricsStreamIdFactory;
  private final MetricsForwarder metricsForwarder;
  private final MemberList memberList;
  private final ShardRegistry shardRegistry;

  private final Gson gson = new Gson();

  public void consumeRecords() throws IOException, IllegalWalEntryException {
    var myShards = shardRegistry.getAssigned();

    for (int shard = 0; shard < CommonConfig.N_SHARDS; shard++) {
      if (myShards.contains(shard)) consume(shard);
      else forward(shard);
    }
  }

  private void consume(int shard) throws IOException {
    var reader = walResourcesPerStream.getWalReader(shard);
    var entry = reader.readNext();
    int consumed = 0;
    var streamId = metricsStreamIdFactory.ofShard(shard);
    while (entry.isPresent() && consumed < batchSize) {
      var unmarshalled =
          gson.fromJson(new String(entry.get().getPayload()), ExportMetricsRequest.class);
      bufferPool.consume(entry.get().getLsn(), streamId, unmarshalled);
      consumed++;
      reader.advance();
      entry = reader.readNext();
    }
  }

  private void forward(int shard) throws IOException {
    var state = shardRegistry.getShardState(shard);
    if (state.getState() != ShardState.STEADY) return;
    var reader = walResourcesPerStream.getWalReader(shard);
    var owner = state.getOwner();
    var member = memberList.getMember(owner);
    var batch = reader.readBatchAndAdvance(batchSize);
    var reqs =
        batch.stream()
            .map(
                (walEntry) ->
                    gson.fromJson(new String(walEntry.getPayload()), ExportMetricsRequest.class))
            .toList();
    var packed = new ForwardedMetricsRequest(shard, reqs);
    metricsForwarder.forward(member.getIp(), member.getPort(), packed);
  }
}
