package org.okapi.traces.query;

import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MultiplexingTraceQueryProcessor implements TraceQueryProcessor {
  // todo: add a configurable thread pool instead of getting everything on a system.
  private final List<TraceQueryProcessor> processors;
  private final ExecutorService pool =
      Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()));

  @Override
  public List<Span> getSpansWithFilter(
      long start, long end, String tenantId, String application, String traceId)
      throws IOException {
    List<Callable<List<Span>>> tasks = new ArrayList<>();
    for (TraceQueryProcessor p : processors) {
      tasks.add(() -> p.getSpansWithFilter(start, end, tenantId, application, traceId));
    }
    return merge(invokeAll(tasks));
  }

  @Override
  public List<Span> getSpansWithFilter(
      long start, long end, String tenantId, String application, AttributeFilter filter)
      throws IOException {
    List<Callable<List<Span>>> tasks = new ArrayList<>();
    for (TraceQueryProcessor p : processors) {
      tasks.add(() -> p.getSpansWithFilter(start, end, tenantId, application, filter));
    }
    return merge(invokeAll(tasks));
  }

  @Override
  public List<Span> getTrace(
      long start, long end, String tenantId, String application, String spanId) throws IOException {
    List<Callable<List<Span>>> tasks = new ArrayList<>();
    for (TraceQueryProcessor p : processors) {
      tasks.add(() -> p.getTrace(start, end, tenantId, application, spanId));
    }
    return merge(invokeAll(tasks));
  }

  private List<List<Span>> invokeAll(List<Callable<List<Span>>> tasks) {
    try {
      List<Future<List<Span>>> futures = pool.invokeAll(tasks);
      List<List<Span>> results = new ArrayList<>(futures.size());
      for (Future<List<Span>> f : futures) {
        try {
          results.add(f.get());
        } catch (ExecutionException e) {
          log.warn("Multiplexed query failed for one processor", e);
          results.add(Collections.emptyList());
        }
      }
      return results;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return List.of();
    }
  }

  private static List<Span> merge(List<List<Span>> lists) {
    Map<String, Span> dedup = new LinkedHashMap<>();
    for (List<Span> l : lists) {
      for (Span s : l) {
        String key = hex(s.getTraceId().toByteArray()) + ":" + hex(s.getSpanId().toByteArray());
        dedup.putIfAbsent(key, s);
      }
    }
    List<Span> merged = new ArrayList<>(dedup.values());
    merged.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));
    return merged;
  }

  private static String hex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) sb.append(String.format("%02x", b));
    return sb.toString();
  }

  @Override
  public void close() {
    for (TraceQueryProcessor p : processors) {
      try {
        p.close();
      } catch (Exception ignored) {
      }
    }
    pool.shutdown();
  }
}
