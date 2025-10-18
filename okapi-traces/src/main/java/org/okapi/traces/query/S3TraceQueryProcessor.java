package org.okapi.traces.query;

import static org.okapi.traces.query.TraceSpanUtils.*;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import java.util.zip.CRC32;
import lombok.extern.slf4j.Slf4j;
import org.okapi.s3.ByteRangeCache;
import org.okapi.traces.metrics.MetricsEmitter;
import org.okapi.traces.metrics.NoopMetricsEmitter;

@Slf4j
public class S3TraceQueryProcessor implements TraceQueryProcessor {

  @FunctionalInterface
  public interface RangeFetcher {
    byte[] getRange(String bucket, String key, long start, long endExclusive) throws Exception;
  }

  @FunctionalInterface
  public interface ObjectLister {
    List<String> list(String bucket, String prefix) throws Exception;
  }

  private final S3TracefileKeyResolver resolver;
  private final RangeFetcher fetcher;
  private final ObjectLister lister;
  private final MetricsEmitter metrics;
  private final ExecutorService pool;

  public S3TraceQueryProcessor(
      ByteRangeCache cache,
      S3TracefileKeyResolver resolver,
      ObjectLister lister,
      TraceQueryConfig config,
      MetricsEmitter metrics) {
    this.resolver = resolver;
    this.fetcher = (b, k, s, e) -> cache.getRange(b, k, s, e);
    this.lister = lister;
    this.metrics = metrics == null ? new NoopMetricsEmitter() : metrics;
    this.pool = Executors.newFixedThreadPool(config.getQueryThreads());
  }

  public S3TraceQueryProcessor(
      RangeFetcher fetcher,
      S3TracefileKeyResolver resolver,
      ObjectLister lister,
      TraceQueryConfig config,
      MetricsEmitter metrics) {
    this.resolver = resolver;
    this.fetcher = fetcher;
    this.lister = lister;
    this.metrics = metrics == null ? new NoopMetricsEmitter() : metrics;
    this.pool = Executors.newFixedThreadPool(config.getQueryThreads());
  }

  @Override
  public List<Span> getSpansWithFilter(
      long start, long end, String tenantId, String application, String traceId)
      throws IOException {
    List<long[]> keys = hourBuckets(start, end);
    if (keys.isEmpty()) return List.of();
    List<Callable<List<Span>>> tasks = new ArrayList<>();
    for (long hb : bucketsFrom(keys)) {
      final String bucket = resolver.bucket();
      for (String prefix : resolver.keyFor(tenantId, application, hb)) {
        List<String> objKeys;
        try {
          objKeys = lister.list(bucket, prefix);
        } catch (Exception e) {
          log.warn("S3 list failed for bucket={}, prefix={}", bucket, prefix, e);
          continue;
        }
        for (String key : objKeys) {
          tasks.add(
              () -> scanFileForTraceId(bucket, key, start, end, tenantId, application, traceId));
        }
      }
    }
    List<Span> result = new ArrayList<>();
    for (Future<List<Span>> f : invokeAll(tasks)) {
      try {
        result.addAll(f.get());
      } catch (ExecutionException | InterruptedException e) {
        log.warn("S3 scan failed", e);
      }
    }
    result.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));
    return result;
  }

  @Override
  public List<Span> getSpansWithFilter(
      long start, long end, String tenantId, String application, AttributeFilter filter)
      throws IOException {
    List<long[]> keys = hourBuckets(start, end);
    if (keys.isEmpty()) return List.of();
    List<Callable<List<Span>>> tasks = new ArrayList<>();
    for (long hb : bucketsFrom(keys)) {
      final String bucket = resolver.bucket();
      for (String prefix : resolver.keyFor(tenantId, application, hb)) {
        List<String> objKeys;
        try {
          objKeys = lister.list(bucket, prefix);
        } catch (Exception e) {
          log.warn("S3 list failed for bucket={}, prefix={}", bucket, prefix, e);
          continue;
        }
        for (String key : objKeys) {
          tasks.add(
              () -> scanFileForAttribute(bucket, key, start, end, tenantId, application, filter));
        }
      }
    }
    List<Span> result = new ArrayList<>();
    for (Future<List<Span>> f : invokeAll(tasks)) {
      try {
        result.addAll(f.get());
      } catch (ExecutionException | InterruptedException e) {
        log.warn("S3 scan failed", e);
      }
    }
    result.sort(Comparator.comparingLong(Span::getStartTimeUnixNano));
    return result;
  }

  @Override
  public List<Span> getTrace(
      long start, long end, String tenantId, String application, String spanId) throws IOException {
    List<long[]> keys = hourBuckets(start, end);
    if (keys.isEmpty()) return List.of();

    var token = new CancellationToken();
    var ecs = new ExecutorCompletionService<Optional<Span>>(pool);
    List<Future<Optional<Span>>> futures = new ArrayList<>();
    for (long hb : bucketsFrom(keys)) {
      final String bucket = resolver.bucket();
      for (String prefix : resolver.keyFor(tenantId, application, hb)) {
        List<String> objKeys;
        try {
          objKeys = lister.list(bucket, prefix);
        } catch (Exception e) {
          log.warn("S3 list failed for bucket={}, prefix={}", bucket, prefix, e);
          continue;
        }
        for (String key : objKeys) {
          futures.add(
              ecs.submit(() -> findSpanInFile(bucket, key, start, end, spanId, token)));
        }
      }
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
        log.warn("S3 scan failed", e);
      }
    }
    if (target == null) return List.of();

    String traceIdHex = bytesToHex(target.getTraceId().toByteArray());
    return getSpansWithFilter(start, end, tenantId, application, traceIdHex);
  }

  private List<Future> invokeAll(List<? extends Callable> tasks) {
    try {
      return (List<Future>) pool.invokeAll((List) tasks);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Collections.emptyList();
    }
  }

  private static List<long[]> hourBuckets(long start, long end) {
    long s = start / 3_600_000L;
    long e = end / 3_600_000L;
    List<long[]> l = new ArrayList<>();
    l.add(new long[] {s, e});
    return l;
  }

  private static List<Long> bucketsFrom(List<long[]> ranges) {
    List<Long> buckets = new ArrayList<>();
    for (long[] r : ranges) for (long i = r[0]; i <= r[1]; i++) buckets.add(i);
    return buckets;
  }

  private List<Span> scanFileForTraceId(
      String bucket,
      String key,
      long start,
      long end,
      String tenantId,
      String application,
      String traceId) {
    List<Span> out = new ArrayList<>();
    long offset = 0;
    while (true) {
      byte[] hdr;
      try {
        hdr = fetcher.getRange(bucket, key, offset, offset + 8);
      } catch (Exception e) {
        log.debug("header fetch failed for {}:{}", bucket, key, e);
        break;
      }
      if (hdr == null || hdr.length < 8) break; // EOF
      int totalLen = readIntBE(hdr, 0);
      int crc32 = readIntBE(hdr, 4);

      // time window pre-filter
      byte[] ts;
      try {
        ts = fetcher.getRange(bucket, key, offset + 8, offset + 8 + 16);
      } catch (Exception e) {
        metrics.emitPageParseError(tenantId, application);
        offset += 8L + totalLen;
        continue;
      }
      if (ts == null || ts.length < 16) {
        offset += 8L + totalLen;
        continue;
      }
      long tsStart = readLongBE(ts, 0);
      long tsEnd = readLongBE(ts, 8);
      metrics.emitPageRead(tenantId, application);
      if (!(tsStart <= end && start <= tsEnd)) {
        metrics.emitPageTimeSkipped(tenantId, application);
        offset += 8L + totalLen;
        continue;
      }

      // Bloom check
      byte[] blenBytes;
      try {
        blenBytes = fetcher.getRange(bucket, key, offset + 8 + 16, offset + 8 + 16 + 4);
      } catch (Exception e) {
        metrics.emitPageParseError(tenantId, application);
        offset += 8L + totalLen;
        continue;
      }
      if (blenBytes == null || blenBytes.length < 4) {
        offset += 8L + totalLen;
        continue;
      }
      int bloomLen = readIntBE(blenBytes, 0);
      byte[] bloomBytes;
      try {
        bloomBytes =
            fetcher.getRange(bucket, key, offset + 8 + 16 + 4, offset + 8 + 16 + 4 + bloomLen);
      } catch (Exception e) {
        metrics.emitPageParseError(tenantId, application);
        offset += 8L + totalLen;
        continue;
      }
      try {
        BloomFilter<CharSequence> bloom =
            BloomFilter.readFrom(
                new java.io.ByteArrayInputStream(bloomBytes),
                Funnels.stringFunnel(StandardCharsets.UTF_8));
        metrics.emitBloomChecked(tenantId, application);
        if (!bloom.mightContain(traceId)) {
          metrics.emitBloomMiss(tenantId, application);
          offset += 8L + totalLen;
          continue;
        }
        metrics.emitBloomHit(tenantId, application);
      } catch (IOException ioe) {
        metrics.emitPageParseError(tenantId, application);
        offset += 8L + totalLen;
        continue;
      }

      // Fetch full payload for CRC and parsing
      byte[] pagePayload;
      try {
        pagePayload = fetcher.getRange(bucket, key, offset + 8, offset + 8 + totalLen);
      } catch (Exception e) {
        metrics.emitPageParseError(tenantId, application);
        offset += 8L + totalLen;
        continue;
      }
      if (!validateCrc(pagePayload, crc32)) {
        metrics.emitPageParseError(tenantId, application);
        offset += 8L + totalLen;
        continue;
      }

      int before = out.size();
      parsePayloadCollectByTraceId(pagePayload, start, end, traceId, out);
      metrics.emitSpansMatched(tenantId, application, out.size() - before);
      metrics.emitPageParsed(tenantId, application);

      offset += 8L + totalLen;
    }
    return out;
  }

  private List<Span> scanFileForAttribute(
      String bucket,
      String key,
      long start,
      long end,
      String tenantId,
      String application,
      AttributeFilter filter) {
    List<Span> out = new ArrayList<>();
    long offset = 0;
    while (true) {
      byte[] hdr;
      try {
        hdr = fetcher.getRange(bucket, key, offset, offset + 8);
      } catch (Exception e) {
        log.debug("header fetch failed for {}:{}", bucket, key, e);
        break;
      }
      if (hdr == null || hdr.length < 8) break; // EOF
      int totalLen = readIntBE(hdr, 0);
      int crc32 = readIntBE(hdr, 4);

      byte[] ts;
      try {
        ts = fetcher.getRange(bucket, key, offset + 8, offset + 8 + 16);
      } catch (Exception e) {
        metrics.emitPageParseError(tenantId, application);
        offset += 8L + totalLen;
        continue;
      }
      if (ts == null || ts.length < 16) {
        offset += 8L + totalLen;
        continue;
      }
      long tsStart = readLongBE(ts, 0);
      long tsEnd = readLongBE(ts, 8);
      metrics.emitPageRead(tenantId, application);
      if (!(tsStart <= end && start <= tsEnd)) {
        metrics.emitPageTimeSkipped(tenantId, application);
        offset += 8L + totalLen;
        continue;
      }

      byte[] pagePayload;
      try {
        pagePayload = fetcher.getRange(bucket, key, offset + 8, offset + 8 + totalLen);
      } catch (Exception e) {
        metrics.emitPageParseError(tenantId, application);
        offset += 8L + totalLen;
        continue;
      }
      if (!validateCrc(pagePayload, crc32)) {
        metrics.emitPageParseError(tenantId, application);
        offset += 8L + totalLen;
        continue;
      }

      int before = out.size();
      parsePayloadCollectByAttribute(pagePayload, start, end, filter, out);
      metrics.emitSpansMatched(tenantId, application, out.size() - before);
      metrics.emitPageParsed(tenantId, application);

      offset += 8L + totalLen;
    }
    return out;
  }

  private Optional<Span> findSpanInFile(
      String bucket, String key, long start, long end, String spanId, CancellationToken token) {
    long offset = 0;
    while (true) {
      if (token.isCancelled() || Thread.currentThread().isInterrupted()) return Optional.empty();
      byte[] hdr;
      try {
        hdr = fetcher.getRange(bucket, key, offset, offset + 8);
      } catch (Exception e) {
        break;
      }
      if (hdr == null || hdr.length < 8) break;
      int totalLen = readIntBE(hdr, 0);
      int crc32 = readIntBE(hdr, 4);
      byte[] ts;
      try {
        ts = fetcher.getRange(bucket, key, offset + 8, offset + 8 + 16);
      } catch (Exception e) {
        offset += 8L + totalLen;
        continue;
      }
      if (ts == null || ts.length < 16) {
        offset += 8L + totalLen;
        continue;
      }
      long tsStart = readLongBE(ts, 0);
      long tsEnd = readLongBE(ts, 8);
      if (!(tsStart <= end && start <= tsEnd)) {
        offset += 8L + totalLen;
        continue;
      }

      byte[] pagePayload;
      try {
        pagePayload = fetcher.getRange(bucket, key, offset + 8, offset + 8 + totalLen);
      } catch (Exception e) {
        offset += 8L + totalLen;
        continue;
      }
      if (!validateCrc(pagePayload, crc32)) {
        offset += 8L + totalLen;
        continue;
      }

      Span found = parsePayloadFindSpanId(pagePayload, start, end, spanId);
      if (found != null) return Optional.of(found);
      offset += 8L + totalLen;
    }
    return Optional.empty();
  }

  private static boolean validateCrc(byte[] pagePayload, int crc32Header) {
    try {
      CRC32 crc = new CRC32();
      crc.update(pagePayload);
      long computed = crc.getValue();
      return ((int) (computed & 0xFFFFFFFFL)) == crc32Header;
    } catch (Exception e) {
      return false;
    }
  }

  private static void parsePayloadCollectByTraceId(
      byte[] pagePayload, long start, long end, String traceId, List<Span> out) {
    int pos = 0;
    long tsStart = readLongBE(pagePayload, pos);
    pos += 8;
    long tsEnd = readLongBE(pagePayload, pos);
    pos += 8;
    int bloomLen = readIntBE(pagePayload, pos);
    pos += 4;
    pos += bloomLen;
    while (pos < pagePayload.length) {
      int len = readIntBE(pagePayload, pos);
      pos += 4;
      byte[] bytes = java.util.Arrays.copyOfRange(pagePayload, pos, pos + len);
      pos += len;
      try {
        ExportTraceServiceRequest req = ExportTraceServiceRequest.parseFrom(bytes);
        for (ResourceSpans rs : req.getResourceSpansList()) {
          for (Object ss : getScopeOrInstrumentationSpans(rs)) {
            for (Span sp : getSpansFromScope(ss)) {
              String tid = bytesToHex(sp.getTraceId().toByteArray());
              if (traceId.equals(tid) && spanOverlaps(sp, start, end)) out.add(sp);
            }
          }
        }
      } catch (Exception ignore) {
      }
    }
  }

  private static void parsePayloadCollectByAttribute(
      byte[] pagePayload, long start, long end, AttributeFilter filter, List<Span> out) {
    int pos = 0;
    long tsStart = readLongBE(pagePayload, pos);
    pos += 8;
    long tsEnd = readLongBE(pagePayload, pos);
    pos += 8;
    int bloomLen = readIntBE(pagePayload, pos);
    pos += 4;
    pos += bloomLen;
    while (pos < pagePayload.length) {
      int len = readIntBE(pagePayload, pos);
      pos += 4;
      byte[] bytes = java.util.Arrays.copyOfRange(pagePayload, pos, pos + len);
      pos += len;
      try {
        ExportTraceServiceRequest req = ExportTraceServiceRequest.parseFrom(bytes);
        for (ResourceSpans rs : req.getResourceSpansList()) {
          if (!resourceMatches(rs, filter)) continue;
          for (Object ss : getScopeOrInstrumentationSpans(rs)) {
            for (Span sp : getSpansFromScope(ss)) {
              if (spanOverlaps(sp, start, end)) out.add(sp);
            }
          }
        }
      } catch (Exception ignore) {
      }
    }
  }

  private static Span parsePayloadFindSpanId(
      byte[] pagePayload, long start, long end, String spanId) {
    int pos = 0;
    long tsStart = readLongBE(pagePayload, pos);
    pos += 8;
    long tsEnd = readLongBE(pagePayload, pos);
    pos += 8;
    int bloomLen = readIntBE(pagePayload, pos);
    pos += 4;
    pos += bloomLen;
    while (pos < pagePayload.length) {
      int len = readIntBE(pagePayload, pos);
      pos += 4;
      byte[] bytes = java.util.Arrays.copyOfRange(pagePayload, pos, pos + len);
      pos += len;
      try {
        ExportTraceServiceRequest req = ExportTraceServiceRequest.parseFrom(bytes);
        for (ResourceSpans rs : req.getResourceSpansList()) {
          for (Object ss : getScopeOrInstrumentationSpans(rs)) {
            for (Span sp : getSpansFromScope(ss)) {
              if (spanId.equals(bytesToHex(sp.getSpanId().toByteArray()))
                  && spanOverlaps(sp, start, end)) return sp;
            }
          }
        }
      } catch (Exception ignore) {
      }
    }
    return null;
  }

  private static int readIntBE(byte[] b, int off) {
    return ((b[off] & 0xFF) << 24)
        | ((b[off + 1] & 0xFF) << 16)
        | ((b[off + 2] & 0xFF) << 8)
        | (b[off + 3] & 0xFF);
  }

  private static long readLongBE(byte[] b, int off) {
    return ((long) (b[off] & 0xFF) << 56)
        | ((long) (b[off + 1] & 0xFF) << 48)
        | ((long) (b[off + 2] & 0xFF) << 40)
        | ((long) (b[off + 3] & 0xFF) << 32)
        | ((long) (b[off + 4] & 0xFF) << 24)
        | ((long) (b[off + 5] & 0xFF) << 16)
        | ((long) (b[off + 6] & 0xFF) << 8)
        | ((long) (b[off + 7] & 0xFF));
  }

  private static boolean resourceMatches(ResourceSpans rs, AttributeFilter filter) {
    var res = rs.getResource();
    for (KeyValue kv : res.getAttributesList()) {
      if (!kv.getKey().equals(filter.getName())) continue;
      String v = anyValueToString(kv.getValue());
      if (filter.getPattern() != null) {
        if (filter.getPattern().matches(v)) return true;
      } else if (filter.getValue() != null) {
        if (filter.getValue().equals(v)) return true;
      }
    }
    return false;
  }

  @Override
  public void close() {
    pool.shutdown();
  }
}
