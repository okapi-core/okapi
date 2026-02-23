/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.service;

import com.google.gson.Gson;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.okapi.CommonConfig;
import org.okapi.abstractio.StreamIdFactory;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.identity.MemberList;
import org.okapi.logs.LogsBufferPool;
import org.okapi.logs.forwarding.LogForwarder;
import org.okapi.logs.io.ForwardedLogIngestRecord;
import org.okapi.logs.io.LogIngestRecord;
import org.okapi.sharding.ShardRegistry;
import org.okapi.sharding.ShardState;
import org.okapi.wal.io.IllegalWalEntryException;

@RequiredArgsConstructor
public class LogsShardWalConsumer {

  private final WalResourcesPerStream<Integer> walResourcesPerStream;
  private final LogsBufferPool bufferPool;
  private final Integer batchSize;
  private final StreamIdFactory streamIdFactory;
  private final LogForwarder logsForwarder;
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
    var streamId = streamIdFactory.withStreamAndShard(shard);
    while (entry.isPresent() && consumed < batchSize) {
      var unmarshalled = gson.fromJson(new String(entry.get().getPayload()), LogIngestRecord.class);
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
                    gson.fromJson(new String(walEntry.getPayload()), LogIngestRecord.class))
            .toList();
    var packed = new ForwardedLogIngestRecord(shard, reqs);
    logsForwarder.forward(member, packed);
  }
}
