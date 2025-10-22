package org.okapi.logs.runtime;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.okapi.logs.config.LogsConfigProperties;
import org.okapi.logs.io.LogFileWriter;
import org.okapi.logs.mappers.OtelToLogMapper;
import org.okapi.logs.io.LogPage;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LogPageBufferPool {
  private final LogsConfigProperties cfg;
  private final LogFileWriter fileWriter;
  private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

  private final Map<Long, ActivePage> pages = new ConcurrentHashMap<>();
  private final BlockingQueue<PendingFlush> flushQueue = new LinkedBlockingQueue<>();

  public LogPageBufferPool(LogsConfigProperties cfg, MeterRegistry meterRegistry) {
    this.cfg = cfg;
    this.fileWriter = new LogFileWriter(cfg);
    this.meterRegistry = meterRegistry;
    final Thread persister =
        new Thread(
            () -> {
              while (!Thread.currentThread().isInterrupted()) {
                try {
                  PendingFlush pf = flushQueue.take();
                  var entry = fileWriter.appendPage(pf.tenantId, pf.logStream, pf.page);
                  meterRegistry.counter("okapi.logs.page_flush_total").increment();
                  meterRegistry
                      .counter("okapi.logs.pages_serialized_bytes")
                      .increment(entry.getLength());
                } catch (InterruptedException ie) {
                  Thread.currentThread().interrupt();
                } catch (Exception e) {
                  log.error("Failed to persist page", e);
                }
              }
            },
            "okapi-log-persister");
    persister.setDaemon(true);
    persister.start();
  }

  public void consume(
      String tenantId, String logStream, long tsMillis, String traceId, int level, String body) {
    long key = hashKey(tenantId, logStream);
    ActivePage ap = pages.computeIfAbsent(key, k -> new ActivePage(tenantId, logStream, cfg));
    ap.append(tsMillis, traceId, level, body);
    if (ap.shouldFlush()) {
      flushPage(key, ap);
    }
  }

  public void consume(
      String tenantId, String logStream, io.opentelemetry.proto.logs.v1.LogRecord record) {
    long tsMs = record.getTimeUnixNano() / 1_000_000L;
    int level = OtelToLogMapper.mapLevel(record.getSeverityNumber().getNumber());
    String traceId = OtelToLogMapper.traceIdToHex(record.getTraceId());
    String text = OtelToLogMapper.anyValueToString(record.getBody());
    consume(tenantId, logStream, tsMs, traceId, level, text);
  }

  private void flushPage(long key, ActivePage ap) {
    Optional<LogPage> page = ap.rotate();
    page.ifPresent(
        p -> {
          flushQueue.offer(new PendingFlush(ap.tenantId, ap.logStream, p));
        });
  }

  private static long hashKey(String tenantId, String logStream) {
    // simple 64-bit hash
    return java.util.Objects.hash(tenantId, logStream);
  }

  public LogPage snapshotActivePage(String tenantId, String logStream) {
    long key = hashKey(tenantId, logStream);
    ActivePage ap = pages.get(key);
    if (ap == null) return null;
    ap.lock.lock();
    try {
      // Return current mutable page reference; caller should treat as read-only
      return ap.page;
    } finally {
      ap.lock.unlock();
    }
  }

  private static final class PendingFlush {
    final String tenantId;
    final String logStream;
    final LogPage page;

    PendingFlush(String tenantId, String logStream, LogPage page) {
      this.tenantId = tenantId;
      this.logStream = logStream;
      this.page = page;
    }
  }

  private static final class ActivePage {
    private final String tenantId;
    private final String logStream;
    private final LogsConfigProperties cfg;
    private final ReentrantLock lock = new ReentrantLock();
    private LogPage page;
    private long firstTs = -1L;
    private int approxBytes = 76 + 4; // header + CRC

    ActivePage(String tenantId, String logStream, LogsConfigProperties cfg) {
      this.tenantId = tenantId;
      this.logStream = logStream;
      this.cfg = cfg;
      this.page =
          LogPage.builder()
              .traceIdSet(
                  BloomFilter.create(
                      Funnels.stringFunnel(StandardCharsets.UTF_8), cfg.getMaxDocsPerPage()))
              .expectedInsertions(cfg.getMaxDocsPerPage())
              .build();
    }

    void append(long tsMillis, String traceId, int level, String body) {
      lock.lock();
      try {
        if (firstTs < 0) firstTs = tsMillis;
        page.append(tsMillis, traceId, level, body);
        int bodyBytes = body != null ? body.getBytes(StandardCharsets.UTF_8).length : 0;
        approxBytes += 4 /*size table entry*/ + bodyBytes + 8 /*proto overhead approx*/;
      } finally {
        lock.unlock();
      }
    }

    boolean shouldFlush() {
      lock.lock();
      try {
        if (page.sizeInDocs() >= cfg.getMaxDocsPerPage()) return true;
        if (approxBytes >= cfg.getMaxPageBytes()) return true;
        long now = Instant.now().toEpochMilli();
        if (firstTs > 0 && now - firstTs >= cfg.getMaxPageWindowMs()) return true;
        return false;
      } finally {
        lock.unlock();
      }
    }

    Optional<LogPage> rotate() {
      lock.lock();
      try {
        LogPage cur = this.page;
        if (cur.sizeInDocs() == 0) return Optional.empty();
        // start a new page
        this.page =
            LogPage.builder()
                .traceIdSet(
                    BloomFilter.create(
                        Funnels.stringFunnel(StandardCharsets.UTF_8), cfg.getMaxDocsPerPage()))
                .expectedInsertions(cfg.getMaxDocsPerPage())
                .build();
        this.firstTs = -1L;
        this.approxBytes = 76 + 4;
        return Optional.of(cur);
      } finally {
        lock.unlock();
      }
    }
  }

  public void flushPagesOlderThan(long boundaryTsMillis) {
    pages.forEach(
        (k, ap) -> {
          ap.lock.lock();
          try {
            if (ap.firstTs > 0 && ap.firstTs < boundaryTsMillis && ap.page.sizeInDocs() > 0) {
              Optional<LogPage> page = ap.rotate();
              page.ifPresent(p -> flushQueue.offer(new PendingFlush(ap.tenantId, ap.logStream, p)));
            }
          } finally {
            ap.lock.unlock();
          }
        });
  }

  public void flushAllNow() {
    pages.forEach(
        (k, ap) -> {
          ap.lock.lock();
          try {
            if (ap.page.sizeInDocs() > 0) {
              Optional<LogPage> page = ap.rotate();
              page.ifPresent(p -> flushQueue.offer(new PendingFlush(ap.tenantId, ap.logStream, p)));
            }
          } finally {
            ap.lock.unlock();
          }
        });
  }

  public void awaitFlushQueueEmpty(long timeoutMillis) {
    long deadline = System.currentTimeMillis() + timeoutMillis;
    while (System.currentTimeMillis() < deadline) {
      if (flushQueue.isEmpty()) return;
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }
}
