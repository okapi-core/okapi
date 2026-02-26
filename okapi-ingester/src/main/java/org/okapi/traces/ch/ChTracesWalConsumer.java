/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.ch;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import org.okapi.metrics.ch.ChConstants;
import org.okapi.metrics.ch.ChWalResources;
import org.okapi.metrics.ch.ChWriter;
import org.okapi.wal.frame.WalEntry;
import org.okapi.wal.io.WalReader;
import org.okapi.wal.manager.WalManager;

@RequiredArgsConstructor
public class ChTracesWalConsumer {
  private final WalReader walReader;
  private final WalManager walManager;
  private final int batchSize;
  private final ChWriter chWriter;
  private final OtelTracesToChRowsConverter converter;
  private final Gson gson = new Gson();
  private final TraceFilterStrategy traceFilterStrategy;
  private final SpanFilterStrategy spanFilterStrategy;

  public ChTracesWalConsumer(
      ChWalResources walResources,
      int batchSize,
      ChWriter chWriter,
      OtelTracesToChRowsConverter converter,
      TraceFilterStrategy traceFilterStrategy,
      SpanFilterStrategy spanFilterStrategy) {
    this.walReader = walResources.getReader();
    this.walManager = walResources.getManager();
    this.batchSize = batchSize;
    this.chWriter = chWriter;
    this.converter = converter;
    this.traceFilterStrategy = Preconditions.checkNotNull(traceFilterStrategy);
    this.spanFilterStrategy = Preconditions.checkNotNull(spanFilterStrategy);
  }

  public void consumeRecords() throws IOException, InterruptedException, ExecutionException {
    var batch = walReader.readBatchAndAdvance(batchSize);
    List<ChSpansTableRow> rows = new ArrayList<>();
    List<ChSpansIngestedAttribsRow> attribRows = new ArrayList<>();
    List<ChServiceRedEvents> redEvents = new ArrayList<>();

    for (var entry : batch) {
      var req = ExportTraceServiceRequest.parseFrom(entry.getPayload());
      if (!traceFilterStrategy.shouldPrune(req)) {
        var pruned = pruneSpans(req);
        if (hasSpans(pruned)) {
          rows.addAll(converter.toRows(pruned));
          attribRows.addAll(converter.toAttributeRows(pruned));
        }
      }
      redEvents.addAll(converter.deriveRedEvents(req));
    }

    Multimap<String, String> writeLoad = ArrayListMultimap.create();
    writeLoad.putAll(ChConstants.TBL_SPANS_V1, rows.stream().map(gson::toJson).toList());
    writeLoad.putAll(
        ChConstants.TBL_SPANS_INGESTED_ATTRIBS, attribRows.stream().map(gson::toJson).toList());
    writeLoad.putAll(
        ChConstants.TBL_SERVICE_RED_EVENTS, redEvents.stream().map(gson::toJson).toList());
    chWriter.writeSyncWithBestEffort(writeLoad);

    var maxLsn = WalEntry.getMaxLsn(batch);
    walManager.commitLsn(maxLsn);
  }

  protected ExportTraceServiceRequest pruneSpans(ExportTraceServiceRequest request) {
    var builder = ExportTraceServiceRequest.newBuilder();
    for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
      var resourceBuilder = ResourceSpans.newBuilder(resourceSpans);
      resourceBuilder.clearScopeSpans();
      for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
        var scopeBuilder = ScopeSpans.newBuilder(scopeSpans);
        scopeBuilder.clearSpans();
        for (Span span : scopeSpans.getSpansList()) {
          if (spanFilterStrategy.shouldPrune(span)) continue;
          scopeBuilder.addSpans(span);
        }
        if (scopeBuilder.getSpansCount() > 0) {
          resourceBuilder.addScopeSpans(scopeBuilder.build());
        }
      }
      if (resourceBuilder.getScopeSpansCount() > 0) {
        builder.addResourceSpans(resourceBuilder.build());
      }
    }
    return builder.build();
  }

  protected boolean hasSpans(ExportTraceServiceRequest request) {
    return request.getResourceSpansList().stream()
        .anyMatch(
            rs -> rs.getScopeSpansList().stream().anyMatch(ss -> !ss.getSpansList().isEmpty()));
  }
}
