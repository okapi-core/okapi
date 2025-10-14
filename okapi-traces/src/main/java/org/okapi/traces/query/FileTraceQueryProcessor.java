package org.okapi.traces.query;

import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.okapi.traces.metrics.NoopMetricsEmitter;

@Slf4j
public class FileTraceQueryProcessor implements TraceQueryProcessor {
  private final TraceFileLocator locator;
  private final TraceFileReader reader;
  private final ExecutorService pool;
  private final TraceQueryConfig config;

  public FileTraceQueryProcessor(Path baseDir) { this(baseDir, TraceQueryConfig.builder().build()); }

  public FileTraceQueryProcessor(Path baseDir, TraceQueryConfig config) {
    this.locator = new TraceFileLocator(baseDir);
    this.reader = new TraceFileReader();
    this.config = config;
    this.pool = Executors.newFixedThreadPool(config.getQueryThreads());
  }

  @Override
  public List<Span> getSpans(long start, long end, String tenantId, String application, String traceId)
      throws IOException {
    List<Path> files = locator.locate(tenantId, application, start, end);
    if (files.isEmpty()) return List.of();
    var metrics = new NoopMetricsEmitter();
    List<Callable<List<Span>>> tasks = new ArrayList<>();
    for (Path f : files) {
      tasks.add(() -> reader.scanForTraceId(f, start, end, tenantId, application, traceId, metrics));
    }
    List<Span> result = new ArrayList<>();
    for (Future<List<Span>> fut : invokeAll(tasks)) {
      try { result.addAll(fut.get()); } catch (ExecutionException e) { log.warn("scan failed", e); }
    }
    result.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));
    return result;
  }

  @Override
  public List<Span> getSpans(
      long start, long end, String tenantId, String application, AttributeFilter filter)
      throws IOException {
    List<Path> files = locator.locate(tenantId, application, start, end);
    if (files.isEmpty()) return List.of();
    var metrics = new NoopMetricsEmitter();
    List<Callable<List<Span>>> tasks = new ArrayList<>();
    for (Path f : files) {
      tasks.add(() -> reader.scanForAttributeFilter(f, start, end, tenantId, application, filter, metrics));
    }
    List<Span> result = new ArrayList<>();
    for (Future<List<Span>> fut : invokeAll(tasks)) {
      try { result.addAll(fut.get()); } catch (ExecutionException e) { log.warn("scan failed", e); }
    }
    result.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));
    return result;
  }

  @Override
  public List<Span> getTrace(long start, long end, String tenantId, String application, String spanId)
      throws IOException {
    List<Path> files = locator.locate(tenantId, application, start, end);
    if (files.isEmpty()) return List.of();

    var token = new CancellationToken();
    var ecs = new ExecutorCompletionService<Optional<Span>>(pool);
    List<Future<Optional<Span>>> futures = new ArrayList<>();
    for (Path f : files) {
      futures.add(ecs.submit(() -> reader.findSpanById(f, start, end, spanId, token)));
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
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (ExecutionException e) {
        log.warn("scan failed", e);
      }
    }
    if (target == null) return List.of();

    String traceIdHex = TraceSpanUtils.bytesToHex(target.getTraceId().toByteArray());
    List<Span> all = getSpans(start, end, tenantId, application, traceIdHex);
    if (all.isEmpty()) return List.of(target);

    var idx = new java.util.HashMap<String, Span>();
    for (Span s : all) idx.put(TraceSpanUtils.bytesToHex(s.getSpanId().toByteArray()), s);
    List<Span> chain = new ArrayList<>();
    Span cur = target;
    while (cur != null) {
      chain.add(cur);
      String parentId = TraceSpanUtils.bytesToHex(cur.getParentSpanId().toByteArray());
      if (parentId == null || parentId.isEmpty()) break;
      cur = idx.get(parentId);
    }
    chain.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));
    return chain;
  }

  private List<Future> invokeAll(List<? extends Callable> tasks) {
    try { return (List<Future>) pool.invokeAll((List) tasks); }
    catch (InterruptedException e) { Thread.currentThread().interrupt(); return Collections.emptyList(); }
  }

  @Override
  public void close() { pool.shutdown(); }
}

