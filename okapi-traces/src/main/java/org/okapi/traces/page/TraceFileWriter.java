package org.okapi.traces.page;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.okapi.traces.metrics.MetricsEmitter;
import org.okapi.traces.metrics.NoopMetricsEmitter;

@Slf4j
public class TraceFileWriter implements AutoCloseable {
  private final Path baseDir;
  private final Map<String, StreamHandle> streams;
  private final TraceWriterConfig config;
  private final ScheduledExecutorService reaper;
  private final MetricsEmitter metrics;

  private static final class StreamHandle {
    final OutputStream os;
    volatile long lastUsedMillis;

    StreamHandle(OutputStream os, long now) {
      this.os = os;
      this.lastUsedMillis = now;
    }
  }

  public TraceFileWriter(Path baseDir) {
    this(baseDir, TraceWriterConfig.builder().build(), new NoopMetricsEmitter());
  }

  public TraceFileWriter(Path baseDir, TraceWriterConfig config, MetricsEmitter metrics) {
    this.baseDir = baseDir;
    this.streams = new ConcurrentHashMap<>();
    this.config = config;
    this.metrics = metrics == null ? new NoopMetricsEmitter() : metrics;
    this.reaper = Executors.newSingleThreadScheduledExecutor();
    this.reaper.scheduleAtFixedRate(
        this::sweepIdle,
        config.getReapIntervalMillis(),
        config.getReapIntervalMillis(),
        TimeUnit.MILLISECONDS);
  }

  public void write(String tenantId, String application, SpanPage page) throws IOException {
    long hourBucket = page.getTsStartMillis() / 3_600_000L;
    String key = tenantId + "::" + application + "::" + hourBucket;
    StreamHandle h = streams.get(key);
    if (h == null) {
      synchronized (this) {
        h = streams.get(key);
        if (h == null) {
          Path dir = baseDir.resolve(tenantId).resolve(application);
          Files.createDirectories(dir);
          Path file = dir.resolve("tracefile." + hourBucket + ".bin");
          OutputStream os =
              Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
          h = new StreamHandle(os, System.currentTimeMillis());
          streams.put(key, h);
          log.debug("Opened writer for {} {} bucket {}", tenantId, application, hourBucket);
        }
      }
    }
    byte[] bytes = page.serialize();
    h.os.write(bytes);
    h.os.flush();
    h.lastUsedMillis = System.currentTimeMillis();
    metrics.emitTracefileWriteBytes(tenantId, application, hourBucket, bytes.length);
  }

  void sweepIdle() {
    long now = System.currentTimeMillis();
    for (Map.Entry<String, StreamHandle> e : streams.entrySet()) {
      StreamHandle h = e.getValue();
      if (now - h.lastUsedMillis >= config.getIdleCloseMillis()) {
        try {
          h.os.close();
        } catch (IOException ex) {
          log.warn("Error closing idle stream {}", e.getKey(), ex);
        }
        streams.remove(e.getKey());
        log.debug("Closed idle writer {}", e.getKey());
      }
    }
  }

  // For tests
  void sweepNow() {
    sweepIdle();
  }

  // For tests
  int openStreamCount() {
    return streams.size();
  }

  @Override
  public void close() {
    reaper.shutdownNow();
    for (Map.Entry<String, StreamHandle> e : streams.entrySet()) {
      try {
        e.getValue().os.close();
      } catch (IOException ignored) {
      }
    }
    streams.clear();
  }
}
