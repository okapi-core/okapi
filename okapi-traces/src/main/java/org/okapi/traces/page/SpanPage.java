package org.okapi.traces.page;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * SpanPage encapsulates a set of OTLP trace payloads with a time window and a Bloom filter of
 * traceIds. Pages are mutable while in-memory; once flushed, do not mutate further.
 */
public final class SpanPage {

  private long tsStartMillis = 0L;
  private long tsEndMillis = 0L;
  private final BloomFilter<CharSequence> traceBloomFilter;
  private final List<ExportTraceServiceRequest> payloads;

  // Running stats
  private int payloadCount;
  private long spanCount;
  private long estimatedPayloadBytes;

  public SpanPage(BloomFilter<CharSequence> traceBloomFilter) {
    this.traceBloomFilter = traceBloomFilter;
    this.payloads = new ArrayList<>();
    this.payloadCount = 0;
    this.spanCount = 0L;
    this.estimatedPayloadBytes = 0L;
  }

  public static SpanPage newEmpty(int expectedInsertions, double fpp) {
    var bloom =
        BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), expectedInsertions, fpp);
    return new SpanPage(bloom);
  }

  public void append(ExportTraceServiceRequest req) {
    Objects.requireNonNull(req, "req");
    updateWindowAndBloom(req);
    payloads.add(req);
    payloadCount += 1;
    spanCount += countSpans(req);
    estimatedPayloadBytes += req.toByteArray().length;
  }

  public long getTsStartMillis() {
    return tsStartMillis;
  }

  public long getTsEndMillis() {
    return tsEndMillis;
  }

  public BloomFilter<CharSequence> getTraceBloomFilter() {
    return traceBloomFilter;
  }

  public List<ExportTraceServiceRequest> getPayloads() {
    return payloads;
  }

  public int getPayloadCount() {
    return payloadCount;
  }

  public long getSpanCount() {
    return spanCount;
  }

  public long getEstimatedPayloadBytes() {
    return estimatedPayloadBytes;
  }

  public long getWindowMillis() {
    if (payloadCount == 0) return 0L;
    if (tsEndMillis < tsStartMillis) return 0L;
    return tsEndMillis - tsStartMillis;
  }

  /**
   * Estimate the serialized size of this page in bytes, including headers but excluding any file
   * container overhead outside this page.
   */
  public long estimatedSerializedSizeBytes() {
    try {
      int bloomBytes = bloomSerializedLength(traceBloomFilter);
      long sumPayloads = 0;
      for (ExportTraceServiceRequest req : payloads) {
        sumPayloads += 4L + req.toByteArray().length; // length prefix + bytes
      }
      return 8L
          + 8L
          + 4L
          + bloomBytes
          + sumPayloads; // tsStart + tsEnd + bloomLen + bloom + payloads
    } catch (IOException e) {
      return -1L;
    }
  }

  public byte[] serialize() throws IOException {
    // Build the inner payload first
    byte[] inner;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos)) {
      dos.writeLong(tsStartMillis);
      dos.writeLong(tsEndMillis);

      // Bloom filter bytes
      ByteArrayOutputStream bloomBos = new ByteArrayOutputStream();
      traceBloomFilter.writeTo(bloomBos);
      byte[] bloomBytes = bloomBos.toByteArray();
      dos.writeInt(bloomBytes.length);
      dos.write(bloomBytes);

      // Payloads
      for (ExportTraceServiceRequest req : payloads) {
        byte[] bytes = req.toByteArray();
        dos.writeInt(bytes.length);
        dos.write(bytes);
      }
      dos.flush();
      inner = bos.toByteArray();
    }

    // CRC32 over inner payload
    CRC32 crc = new CRC32();
    crc.update(inner);
    long crcValue = crc.getValue();

    try (ByteArrayOutputStream outerBos = new ByteArrayOutputStream();
        DataOutputStream outer = new DataOutputStream(outerBos)) {
      outer.writeInt(inner.length);
      outer.writeInt((int) (crcValue & 0xFFFFFFFFL));
      outer.write(inner);
      outer.flush();
      return outerBos.toByteArray();
    }
  }

  public static SpanPage deserialize(byte[] data) throws IOException {
    Objects.requireNonNull(data, "data");
    try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
      int totalLength = dis.readInt();
      int crc32 = dis.readInt();
      byte[] inner = dis.readNBytes(totalLength);

      CRC32 crc = new CRC32();
      crc.update(inner);
      long computed = crc.getValue();
      if (((int) (computed & 0xFFFFFFFFL)) != crc32) {
        throw new IOException("CRC32 mismatch during SpanPage deserialization");
      }

      try (DataInputStream payloadIn = new DataInputStream(new ByteArrayInputStream(inner))) {
        long tsStart = payloadIn.readLong();
        long tsEnd = payloadIn.readLong();
        int bloomLen = payloadIn.readInt();
        byte[] bloomBytes = payloadIn.readNBytes(bloomLen);
        BloomFilter<CharSequence> bloom =
            BloomFilter.readFrom(
                new ByteArrayInputStream(bloomBytes), Funnels.stringFunnel(StandardCharsets.UTF_8));

        SpanPage page = new SpanPage(bloom);
        page.tsStartMillis = tsStart;
        page.tsEndMillis = tsEnd;

        while (payloadIn.available() > 0) {
          int len = payloadIn.readInt();
          byte[] bytes = payloadIn.readNBytes(len);
          ExportTraceServiceRequest req = ExportTraceServiceRequest.parseFrom(bytes);
          page.payloads.add(req);
          page.payloadCount += 1;
          page.spanCount += countSpans(req);
          page.estimatedPayloadBytes += req.toByteArray().length;
        }
        return page;
      }
    }
  }

  private void updateWindowAndBloom(ExportTraceServiceRequest req) {
    for (var rs : req.getResourceSpansList()) {
      for (Object ss : getScopeOrInstrumentationSpans(rs)) {
        for (io.opentelemetry.proto.trace.v1.Span sp : getSpansFromScope(ss)) {
          long startMs = sp.getStartTimeUnixNano() / 1_000_000L;
          long endMs = sp.getEndTimeUnixNano() / 1_000_000L;
          if (payloadCount == 0 && tsStartMillis == 0L && startMs > 0) {
            tsStartMillis = startMs;
          } else if (startMs > 0) {
            tsStartMillis = Math.min(tsStartMillis, startMs);
          }
          if (endMs > 0) tsEndMillis = Math.max(tsEndMillis, endMs);
          String traceId = bytesToHex(sp.getTraceId().toByteArray());
          traceBloomFilter.put(traceId);
        }
      }
    }
  }

  // Helpers
  private static List<?> getScopeOrInstrumentationSpans(
      io.opentelemetry.proto.trace.v1.ResourceSpans rs) {
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
  private static List<io.opentelemetry.proto.trace.v1.Span> getSpansFromScope(Object scope) {
    try {
      var m = scope.getClass().getMethod("getSpansList");
      return (List<io.opentelemetry.proto.trace.v1.Span>) m.invoke(scope);
    } catch (Exception e) {
      return List.of();
    }
  }

  private static long countSpans(ExportTraceServiceRequest req) {
    long count = 0L;
    for (var rs : req.getResourceSpansList()) {
      for (Object ss : getScopeOrInstrumentationSpans(rs)) {
        count += getSpansFromScope(ss).size();
      }
    }
    return count;
  }

  private static int bloomSerializedLength(BloomFilter<CharSequence> bloom) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    bloom.writeTo(bos);
    return bos.size();
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
