/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.exceptions.BadRequestException;
import org.okapi.otel.ResourceAttributesReader;
import org.okapi.sharding.ShardAssigner;
import org.okapi.traces.config.TracesCfg;
import org.okapi.traces.io.ForwardedSpanRecord;
import org.okapi.traces.io.SpanIngestionRecord;
import org.okapi.wal.frame.WalEntry;
import org.okapi.wal.io.IllegalWalEntryException;

@Slf4j
@RequiredArgsConstructor
public class TracesIngester {

  private final TracesCfg tracesCfg;
  private final WalResourcesPerStream<Integer> walResourcesPerStream;
  private final ShardAssigner<String> shardAssigner;

  public void ingest(ExportTraceServiceRequest req)
      throws BadRequestException, IllegalWalEntryException, IOException {
    for (var resourceSpans : req.getResourceSpansList()) {
      var maybeSvc = ResourceAttributesReader.getSvc(resourceSpans.getResource());
      if (maybeSvc.isEmpty()) continue;
      var svc = maybeSvc.get();
      var groups = ArrayListMultimap.<Integer, Span>create();
      for (var scopeSpans : resourceSpans.getScopeSpansList()) {
        groupByAndMerge(svc, groups, scopeSpans.getSpansList());
      }
      consumeGroup(svc, groups);
    }
  }

  public void groupByAndMerge(String svc, Multimap<Integer, Span> groups, List<Span> spans) {
    for (var span : spans) {
      var tsNanos = span.getStartTimeUnixNano();
      var block = tsNanos / 1_000_000 / tracesCfg.getIdxExpiryDuration();
      var shard = shardAssigner.getShardForStream(svc, block);
      groups.put(shard, span);
    }
  }

  protected void consumeGroup(String svc, Multimap<Integer, Span> groups)
      throws IllegalWalEntryException, IOException {
    for (var shard : groups.keys()) {
      var batch = groups.get(shard);
      var walEntries =
          batch.stream()
              .map(
                  record -> {
                    try {
                      var spanIngestionRecord = SpanIngestionRecord.from(svc, record);
                      return this.toWalEntry(shard, spanIngestionRecord);
                    } catch (IOException e) {
                      throw new RuntimeException(e);
                    }
                  })
              .toList();
      var writer = walResourcesPerStream.getWalWriter(shard);
      writer.appendBatch(walEntries);
    }
  }

  public WalEntry toWalEntry(Integer shard, SpanIngestionRecord span) throws IOException {
    var lsnSupplier = walResourcesPerStream.getLsnSupplier(shard);
    var lsn = lsnSupplier.getLsn();
    var cmd = span.toByteArray();
    return new WalEntry(lsn, cmd);
  }

  public void ingestForwarded(ForwardedSpanRecord forwardedSpanRecord)
      throws IllegalWalEntryException, IOException {
    var walBatch =
        forwardedSpanRecord.getRecords().stream()
            .map(
                r -> {
                  try {
                    return toWalEntry(forwardedSpanRecord.getShard(), r);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                })
            .toList();
    walResourcesPerStream.getWalWriter(forwardedSpanRecord.getShard()).appendBatch(walBatch);
  }
}
