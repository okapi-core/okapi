/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.service;

import com.google.gson.Gson;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.okapi.CommonConfig;
import org.okapi.abstractio.StreamIdFactory;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.identity.MemberList;
import org.okapi.io.StreamReadingException;
import org.okapi.sharding.ShardRegistry;
import org.okapi.sharding.ShardState;
import org.okapi.traces.TracesBufferPool;
import org.okapi.traces.io.ForwardedSpanRecord;
import org.okapi.traces.io.SpanIngestionRecord;
import org.okapi.wal.io.IllegalWalEntryException;

@RequiredArgsConstructor
public class TracesShardWalConsumer {
  private final WalResourcesPerStream<Integer> walResourcesPerStream;
  private final TracesBufferPool bufferPool;
  private final Integer batchSize;
  private final StreamIdFactory streamIdFactory;
  private final HttpTraceForwarder traceForwarder;
  private final MemberList memberList;
  private final ShardRegistry shardRegistry;

  private final Gson gson = new Gson();

  public void consumeRecords()
      throws IOException, IllegalWalEntryException, StreamReadingException {
    var myShards = shardRegistry.getAssigned();

    for (int shard = 0; shard < CommonConfig.N_SHARDS; shard++) {
      if (myShards.contains(shard)) consume(shard);
      else forward(shard);
    }
  }

  private void consume(int shard) throws IOException, StreamReadingException {
    var reader = walResourcesPerStream.getWalReader(shard);
    var entry = reader.readNext();
    int consumed = 0;
    var streamId = streamIdFactory.withStreamAndShard(shard);
    while (entry.isPresent() && consumed < batchSize) {
      var unmarshalled = SpanIngestionRecord.fromByteArray(entry.get().getPayload());
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
                (walEntry) -> {
                  try {
                    return SpanIngestionRecord.fromByteArray(walEntry.getPayload());
                  } catch (StreamReadingException | IOException e) {
                    throw new RuntimeException(e);
                  }
                })
            .toList();
    var packed = new ForwardedSpanRecord(shard, reqs);
    traceForwarder.forward(member, packed);
  }
}
