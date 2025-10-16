package org.okapi.traces.query;

import static org.okapi.traces.query.TraceSpanUtils.*;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.okapi.traces.metrics.MetricsEmitter;
import org.okapi.traces.metrics.NoopMetricsEmitter;
import org.okapi.traces.page.BufferPoolManager;
import org.okapi.traces.page.SpanPage;

@Slf4j
public class InMemoryTraceQueryProcessor implements TraceQueryProcessor {
  private final BufferPoolManager bufferPool;
  private final TraceQueryConfig config;
  private final MetricsEmitter metrics;
  private final ExecutorService pool =
      Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()));

  public InMemoryTraceQueryProcessor(BufferPoolManager bufferPool) {
    this(bufferPool, TraceQueryConfig.builder().build(), new NoopMetricsEmitter());
  }

  public InMemoryTraceQueryProcessor(
      BufferPoolManager bufferPool, TraceQueryConfig config, MetricsEmitter metrics) {
    this.bufferPool = bufferPool;
    this.config = config;
    this.metrics = metrics == null ? new NoopMetricsEmitter() : metrics;
  }

  @Override
  public List<Span> getSpans(
      long start, long end, String tenantId, String application, String traceId)
      throws IOException {
    List<SpanPage> pages = bufferPool.listPages(tenantId, application);
    List<Span> out = new ArrayList<>();
    for (SpanPage page : pages) {
      metrics.emitPageRead(tenantId, application);
      if (!overlaps(page.getTsStartMillis(), page.getTsEndMillis(), start, end)) {
        metrics.emitPageTimeSkipped(tenantId, application);
        continue;
      }
      metrics.emitBloomChecked(tenantId, application);
      if (!page.getTraceBloomFilter().mightContain(traceId)) {
        metrics.emitBloomMiss(tenantId, application);
        continue;
      }
      metrics.emitBloomHit(tenantId, application);
      int before = out.size();
      scanPageForTraceId(page, start, end, traceId, out);
      metrics.emitSpansMatched(tenantId, application, out.size() - before);
      metrics.emitPageParsed(tenantId, application);
    }
    out.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));
    return out;
  }

  @Override
  public List<Span> getSpans(
      long start, long end, String tenantId, String application, AttributeFilter filter)
      throws IOException {
    List<SpanPage> pages = bufferPool.listPages(tenantId, application);
    List<Span> out = new ArrayList<>();
    for (SpanPage page : pages) {
      metrics.emitPageRead(tenantId, application);
      if (!overlaps(page.getTsStartMillis(), page.getTsEndMillis(), start, end)) {
        metrics.emitPageTimeSkipped(tenantId, application);
        continue;
      }
      int before = out.size();
      scanPageForAttribute(page, start, end, filter, out);
      metrics.emitSpansMatched(tenantId, application, out.size() - before);
      metrics.emitPageParsed(tenantId, application);
    }
    out.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));
    return out;
  }

  @Override
  public List<Span> getTrace(
      long start, long end, String tenantId, String application, String spanId) throws IOException {
    List<SpanPage> pages = bufferPool.listPages(tenantId, application);
    var token = new CancellationToken();
    var ecs = new ExecutorCompletionService<Optional<Span>>(pool);
    List<Future<Optional<Span>>> futures = new ArrayList<>();
    for (SpanPage page : pages) {
      futures.add(ecs.submit(() -> findSpanInPage(page, start, end, spanId, token)));
    }
    Span target = null;
    int remaining = futures.size();
    while (remaining-- > 0) {
      try {
        Future<Optional<Span>> fut = ecs.take();
        Optional<Span> s = fut.get();
        if (s.isPresent()) {
          target = s.get();
          token.cancel();
          for (Future<Optional<Span>> other : futures) other.cancel(true);
          break;
        }
      } catch (Exception e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    if (target == null) return List.of();

    String traceIdHex = bytesToHex(target.getTraceId().toByteArray());
    List<Span> all = getSpans(start, end, tenantId, application, traceIdHex);
    if (all.isEmpty()) return List.of(target);
    var idx = new java.util.HashMap<String, Span>();
    for (Span s : all) idx.put(bytesToHex(s.getSpanId().toByteArray()), s);
    List<Span> chain = new ArrayList<>();
    Span cur = target;
    while (cur != null) {
      chain.add(cur);
      String parentId = bytesToHex(cur.getParentSpanId().toByteArray());
      if (parentId == null || parentId.isEmpty()) break;
      cur = idx.get(parentId);
    }
    chain.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));
    return chain;
  }

  private static boolean overlaps(long aStart, long aEnd, long bStart, long bEnd) {
    return aStart <= bEnd && bStart <= aEnd;
  }

  private static void scanPageForTraceId(
      SpanPage page, long start, long end, String traceId, List<Span> out) {
    for (ExportTraceServiceRequest req : page.getPayloads()) {
      for (ResourceSpans rs : req.getResourceSpansList()) {
        for (Object ss : TraceSpanUtils.getScopeOrInstrumentationSpans(rs)) {
          for (Span sp : TraceSpanUtils.getSpansFromScope(ss)) {
            String tid = bytesToHex(sp.getTraceId().toByteArray());
            if (traceId.equals(tid) && spanOverlaps(sp, start, end)) out.add(sp);
          }
        }
      }
    }
  }

  private static void scanPageForAttribute(
      SpanPage page, long start, long end, AttributeFilter filter, List<Span> out) {
    for (ExportTraceServiceRequest req : page.getPayloads()) {
      for (ResourceSpans rs : req.getResourceSpansList()) {
        if (!resourceMatches(rs, filter)) continue;
        for (Object ss : TraceSpanUtils.getScopeOrInstrumentationSpans(rs)) {
          for (Span sp : TraceSpanUtils.getSpansFromScope(ss)) {
            if (spanOverlaps(sp, start, end)) out.add(sp);
          }
        }
      }
    }
  }

  private static boolean resourceMatches(ResourceSpans rs, AttributeFilter filter) {
    var res = rs.getResource();
    for (KeyValue kv : res.getAttributesList()) {
      if (!kv.getKey().equals(filter.getName())) continue;
      String v = TraceSpanUtils.anyValueToString(kv.getValue());
      if (filter.getPattern() != null) {
        if (filter.getPattern().matches(v)) return true;
      } else if (filter.getValue() != null) {
        if (filter.getValue().equals(v)) return true;
      }
    }
    return false;
  }

  private static Optional<Span> findSpanInPage(
      SpanPage page, long start, long end, String spanId, CancellationToken token) {
    if (!overlaps(page.getTsStartMillis(), page.getTsEndMillis(), start, end))
      return Optional.empty();
    for (ExportTraceServiceRequest req : page.getPayloads()) {
      if (token.isCancelled() || Thread.currentThread().isInterrupted()) return Optional.empty();
      for (ResourceSpans rs : req.getResourceSpansList()) {
        for (Object ss : TraceSpanUtils.getScopeOrInstrumentationSpans(rs)) {
          for (Span sp : TraceSpanUtils.getSpansFromScope(ss)) {
            if (spanId.equals(bytesToHex(sp.getSpanId().toByteArray()))
                && spanOverlaps(sp, start, end)) {
              return Optional.of(sp);
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public void close() {
    pool.shutdown();
  }
}
