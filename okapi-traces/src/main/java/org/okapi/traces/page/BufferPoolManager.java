package org.okapi.traces.page;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages mutable SpanPages per (tenant, application, hourBucket). Thread-safe with per-key locks.
 */
public class BufferPoolManager {

  record Key(String tenantId, String application, long hourBucket) {}

  private final ConcurrentMap<Key, SpanPage> pages = new ConcurrentHashMap<>();
  private final ConcurrentMap<Key, ReentrantLock> latches = new ConcurrentHashMap<>();

  private final FlushStrategy flushStrategy;
  private final TraceFileWriter writer;
  private final WriteFailedListener writeFailedListener;

  private final int expectedInsertions;
  private final double fpp;

  public BufferPoolManager(
      FlushStrategy flushStrategy,
      TraceFileWriter writer,
      WriteFailedListener writeFailedListener,
      int expectedInsertions,
      double fpp) {
    this.flushStrategy = Objects.requireNonNull(flushStrategy);
    this.writer = Objects.requireNonNull(writer);
    this.writeFailedListener = Objects.requireNonNull(writeFailedListener);
    this.expectedInsertions = expectedInsertions;
    this.fpp = fpp;
  }

  public void consume(String tenantId, String application, ExportTraceServiceRequest payload) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(application, "application");
    Objects.requireNonNull(payload, "payload");

    long minStartMillis = findMinStartMillis(payload);
    long hourBucket = minStartMillis / 3_600_000L;
    Key key = new Key(tenantId, application, hourBucket);

    ReentrantLock lock = latches.computeIfAbsent(key, k -> new ReentrantLock());
    SpanPage toFlush = null;

    lock.lock();
    try {
      SpanPage page = pages.computeIfAbsent(key, k -> SpanPage.newEmpty(expectedInsertions, fpp));
      page.append(payload);
      if (flushStrategy.shouldFlush(page)) {
        // swap out the page for a new one and flush the old one
        toFlush = page;
        pages.put(key, SpanPage.newEmpty(expectedInsertions, fpp));
      }
    } finally {
      lock.unlock();
    }

    if (toFlush != null) {
      try {
        writer.write(tenantId, application, toFlush);
      } catch (Exception e) {
        writeFailedListener.onWriteFaile(tenantId, application, toFlush);
      }
    }
  }

  private static long findMinStartMillis(ExportTraceServiceRequest req) {
    long min = Long.MAX_VALUE;
    for (var rs : req.getResourceSpansList()) {
      for (Object ss : getScopeOrInstrumentationSpans(rs)) {
        for (io.opentelemetry.proto.trace.v1.Span sp : getSpansFromScope(ss)) {
          long startMs = sp.getStartTimeUnixNano() / 1_000_000L;
          if (startMs > 0) min = Math.min(min, startMs);
        }
      }
    }
    return min == Long.MAX_VALUE ? 0L : min;
  }

  @SuppressWarnings("unchecked")
  private static java.util.List<io.opentelemetry.proto.trace.v1.Span> getSpansFromScope(
      Object scope) {
    try {
      var m = scope.getClass().getMethod("getSpansList");
      return (java.util.List<io.opentelemetry.proto.trace.v1.Span>) m.invoke(scope);
    } catch (Exception e) {
      return java.util.List.of();
    }
  }

  private static java.util.List<?> getScopeOrInstrumentationSpans(
      io.opentelemetry.proto.trace.v1.ResourceSpans rs) {
    try {
      var m = rs.getClass().getMethod("getScopeSpansList");
      return (java.util.List<?>) m.invoke(rs);
    } catch (NoSuchMethodException e) {
      try {
        var m = rs.getClass().getMethod("getInstrumentationLibrarySpansList");
        return (java.util.List<?>) m.invoke(rs);
      } catch (Exception ex) {
        return java.util.List.of();
      }
    } catch (Exception e) {
      return java.util.List.of();
    }
  }

  // Expose a loose-consistency snapshot of current pages for a tenant/application
  public java.util.List<SpanPage> listPages(String tenantId, String application) {
    var out = new java.util.ArrayList<SpanPage>();
    for (var e : pages.entrySet()) {
      var k = e.getKey();
      if (k.tenantId().equals(tenantId) && k.application().equals(application)) {
        SpanPage p = e.getValue();
        if (p != null) out.add(p);
      }
    }
    return out;
  }
}
