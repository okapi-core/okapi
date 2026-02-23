package org.okapi.traces.ch;

import com.google.common.base.Preconditions;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.List;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.ch.ChWalResources;
import org.okapi.wal.frame.WalEntry;
import org.okapi.wal.io.IllegalWalEntryException;

public class ChTracesIngester {
  private final ChWalResources walResources;
  private final TraceFilterStrategy traceFilterStrategy;
  private final SpanFilterStrategy spanFilterStrategy;

  public ChTracesIngester(
      ChWalResources walResources,
      TraceFilterStrategy traceFilterStrategy,
      SpanFilterStrategy spanFilterStrategy) {
    this.walResources = walResources;
    this.traceFilterStrategy = Preconditions.checkNotNull(traceFilterStrategy);
    this.spanFilterStrategy = Preconditions.checkNotNull(spanFilterStrategy);
  }

  public void ingest(ExportTraceServiceRequest request)
      throws BadRequestException, IllegalWalEntryException, IOException {
    ChTracesValidator.validate(request);
    if (traceFilterStrategy.shouldPrune(request)) return;
    var filtered = pruneSpans(request);
    if (!hasSpans(filtered)) return;
    walResources.getWriter().appendBatch(List.of(toWalEntry(filtered)));
  }

  private ExportTraceServiceRequest pruneSpans(ExportTraceServiceRequest request) {
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

  private boolean hasSpans(ExportTraceServiceRequest request) {
    return request.getResourceSpansList().stream()
        .anyMatch(
            rs -> rs.getScopeSpansList().stream().anyMatch(ss -> !ss.getSpansList().isEmpty()));
  }

  private WalEntry toWalEntry(ExportTraceServiceRequest request) throws IOException {
    var lsnSupplier = this.walResources.getSupplier();
    return new WalEntry(lsnSupplier.next(), request.toByteArray());
  }
}
