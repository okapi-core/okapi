package org.okapi.traces.query;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.CRC32;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TraceFileReader {

public List<Span> scanForTraceId(
      Path file, long start, long end, String tenantId, String application, String traceIdHex,
      org.okapi.traces.metrics.MetricsEmitter metrics)
      throws IOException {
    Objects.requireNonNull(traceIdHex, "traceIdHex");
    List<Span> out = new ArrayList<>();
    try (DataInputStream in = new DataInputStream(new FileInputStream(file.toFile()))) {
      while (true) {
        byte[] inner;
        try {
          inner = readNextInner(in);
        } catch (IOException e) {
          log.warn("CRC/read error in file {}. Skipping page.", file, e);
          if (metrics != null) metrics.emitPageParseError(tenantId, application);
          continue;
        }
        if (inner == null) break; // EOF
        try (DataInputStream pIn = new DataInputStream(new ByteArrayInputStream(inner))) {
          long tsStart = pIn.readLong();
          long tsEnd = pIn.readLong();
          if (metrics != null) metrics.emitPageRead(tenantId, application);
          if (!overlaps(tsStart, tsEnd, start, end)) {
            if (metrics != null) metrics.emitPageTimeSkipped(tenantId, application);
            continue; // skip page
          }
          int bloomLen = pIn.readInt();
          byte[] bloomBytes = pIn.readNBytes(bloomLen);
          BloomFilter<CharSequence> bloom =
              BloomFilter.readFrom(
                  new ByteArrayInputStream(bloomBytes),
                  Funnels.stringFunnel(StandardCharsets.UTF_8));
          if (metrics != null) metrics.emitBloomChecked(tenantId, application);
          if (!bloom.mightContain(traceIdHex)) {
            if (metrics != null) metrics.emitBloomMiss(tenantId, application);
            continue; // bloom negative => skip payloads
          }
          if (metrics != null) metrics.emitBloomHit(tenantId, application);

          while (pIn.available() > 0) {
            int len = pIn.readInt();
            byte[] bytes = pIn.readNBytes(len);
            ExportTraceServiceRequest req = ExportTraceServiceRequest.parseFrom(bytes);
            int before = out.size();
            extractSpansByTraceId(req, traceIdHex, start, end, out);
            if (metrics != null) metrics.emitSpansMatched(tenantId, application, out.size() - before);
          }
          if (metrics != null) metrics.emitPageParsed(tenantId, application);
        } catch (Exception e) {
          log.warn("Failed to parse page in file {}. Skipping.", file, e);
          if (metrics != null) metrics.emitPageParseError(tenantId, application);
        }
      }
    } catch (EOFException eof) {
      // done
    }
    return out;
  }

  public List<Span> scanForAttributeFilter(
      Path file, long start, long end, String tenantId, String application,
      AttributeFilter filter, org.okapi.traces.metrics.MetricsEmitter metrics)
      throws IOException {
    List<Span> out = new ArrayList<>();
    try (DataInputStream in = new DataInputStream(new FileInputStream(file.toFile()))) {
      while (true) {
        byte[] inner;
        try {
          inner = readNextInner(in);
        } catch (IOException e) {
          log.warn("CRC/read error in file {}. Skipping page.", file, e);
          if (metrics != null) metrics.emitPageParseError(tenantId, application);
          continue;
        }
        if (inner == null) break; // EOF
        try (DataInputStream pIn = new DataInputStream(new ByteArrayInputStream(inner))) {
          long tsStart = pIn.readLong();
          long tsEnd = pIn.readLong();
          if (metrics != null) metrics.emitPageRead(tenantId, application);
          int bloomLen = pIn.readInt();
          pIn.readNBytes(bloomLen); // skip bloom
          if (!overlaps(tsStart, tsEnd, start, end)) {
            if (metrics != null) metrics.emitPageTimeSkipped(tenantId, application);
            continue; // skip page
          }
          while (pIn.available() > 0) {
            int len = pIn.readInt();
            byte[] bytes = pIn.readNBytes(len);
            ExportTraceServiceRequest req = ExportTraceServiceRequest.parseFrom(bytes);
            int before = out.size();
            extractSpansByAttribute(req, start, end, filter, out);
            if (metrics != null) metrics.emitSpansMatched(tenantId, application, out.size() - before);
          }
          if (metrics != null) metrics.emitPageParsed(tenantId, application);
        } catch (Exception e) {
          log.warn("Failed to parse page in file {}. Skipping.", file, e);
          if (metrics != null) metrics.emitPageParseError(tenantId, application);
        }
      }
    } catch (EOFException eof) {
      // done
    }
    return out;
  }

  public Optional<Span> findSpanById(
      Path file, long start, long end, String spanIdHex, CancellationToken token)
      throws IOException {
    Objects.requireNonNull(spanIdHex, "spanIdHex");
    try (DataInputStream in = new DataInputStream(new FileInputStream(file.toFile()))) {
      while (true) {
        if (token != null && (token.isCancelled() || Thread.currentThread().isInterrupted())) {
          return Optional.empty();
        }
        byte[] inner;
        try {
          inner = readNextInner(in);
        } catch (IOException e) {
          log.warn("CRC/read error in file {}. Skipping page.", file, e);
          continue;
        }
        if (inner == null) break; // EOF
        try (DataInputStream pIn = new DataInputStream(new ByteArrayInputStream(inner))) {
          long tsStart = pIn.readLong();
          long tsEnd = pIn.readLong();
          int bloomLen = pIn.readInt();
          pIn.readNBytes(bloomLen); // skip bloom
          if (!overlaps(tsStart, tsEnd, start, end)) {
            continue; // skip page
          }
          while (pIn.available() > 0) {
            if (token != null && (token.isCancelled() || Thread.currentThread().isInterrupted())) {
              return Optional.empty();
            }
            int len = pIn.readInt();
            byte[] bytes = pIn.readNBytes(len);
            ExportTraceServiceRequest req = ExportTraceServiceRequest.parseFrom(bytes);
            var found = findSpanInReqBySpanId(req, spanIdHex, start, end);
            if (found != null) return Optional.of(found);
          }
        } catch (Exception e) {
          log.warn("Failed to parse page in file {}. Skipping.", file, e);
        }
      }
    } catch (EOFException eof) {
      // done
    }
    return Optional.empty();
  }

  private static byte[] readNextInner(DataInputStream in) throws IOException {
    try {
      int totalLength = in.readInt();
      int crc32 = in.readInt();
      byte[] inner = in.readNBytes(totalLength);
      if (inner.length < totalLength) return null; // truncated
      CRC32 crc = new CRC32();
      crc.update(inner);
      long computed = crc.getValue();
      if (((int) (computed & 0xFFFFFFFFL)) != crc32) {
        // caller logs context; return null here to allow skip? better to throw to allow caller log
        throw new IOException("CRC32 mismatch");
      }
      return inner;
    } catch (EOFException eof) {
      return null;
    }
  }

  private static boolean overlaps(long aStart, long aEnd, long bStart, long bEnd) {
    return aStart <= bEnd && bStart <= aEnd;
  }

  private static void extractSpansByTraceId(
      ExportTraceServiceRequest req, String traceIdHex, long start, long end, List<Span> out) {
    for (var rs : req.getResourceSpansList()) {
      for (Object ss : getScopeOrInstrumentationSpans(rs)) {
        for (Span sp : getSpansFromScope(ss)) {
          String tid = bytesToHex(sp.getTraceId().toByteArray());
          if (traceIdHex.equals(tid) && spanOverlaps(sp, start, end)) {
            out.add(sp);
          }
        }
      }
    }
  }

  private static void extractSpansByAttribute(
      ExportTraceServiceRequest req, long start, long end, AttributeFilter filter, List<Span> out) {
    for (ResourceSpans rs : req.getResourceSpansList()) {
      if (!resourceMatches(rs, filter)) continue;
      for (Object ss : getScopeOrInstrumentationSpans(rs)) {
        for (Span sp : getSpansFromScope(ss)) {
          if (spanOverlaps(sp, start, end)) out.add(sp);
        }
      }
    }
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

  private static Span findSpanInReqBySpanId(
      ExportTraceServiceRequest req, String spanIdHex, long start, long end) {
    for (var rs : req.getResourceSpansList()) {
      for (Object ss : getScopeOrInstrumentationSpans(rs)) {
        for (Span sp : getSpansFromScope(ss)) {
          if (spanIdHex.equals(bytesToHex(sp.getSpanId().toByteArray())) && spanOverlaps(sp, start, end)) {
            return sp;
          }
        }
      }
    }
    return null;
  }

  private static boolean spanOverlaps(Span sp, long start, long end) {
    long s = sp.getStartTimeUnixNano() / 1_000_000L;
    long e = sp.getEndTimeUnixNano() / 1_000_000L;
    return s <= end && start <= e;
  }

  private static List<?> getScopeOrInstrumentationSpans(ResourceSpans rs) {
    try {
      var m = rs.getClass().getMethod("getScopeSpansList");
      return (List<?>) m.invoke(rs);
    } catch (NoSuchMethodException e) {
      try {
        var m = rs.getClass().getMethod("getInstrumentationLibrarySpansList");
        return (List<?>) m.invoke(rs);
      } catch (Exception ex) {
        return List.of();
      }
    } catch (Exception e) {
      return List.of();
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Span> getSpansFromScope(Object scope) {
    try {
      var m = scope.getClass().getMethod("getSpansList");
      return (List<Span>) m.invoke(scope);
    } catch (Exception e) {
      return List.of();
    }
  }

  private static String anyValueToString(AnyValue v) {
    return switch (v.getValueCase()) {
      case STRING_VALUE -> v.getStringValue();
      case BOOL_VALUE -> Boolean.toString(v.getBoolValue());
      case INT_VALUE -> Long.toString(v.getIntValue());
      case DOUBLE_VALUE -> Double.toString(v.getDoubleValue());
      case ARRAY_VALUE -> v.getArrayValue().getValuesList().toString();
      case KVLIST_VALUE -> v.getKvlistValue().getValuesList().toString();
      case BYTES_VALUE -> bytesToHex(v.getBytesValue().toByteArray());
      case VALUE_NOT_SET -> "";
    };
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
