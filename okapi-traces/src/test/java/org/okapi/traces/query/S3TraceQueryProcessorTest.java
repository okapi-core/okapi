package org.okapi.traces.query;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.traces.page.SpanPage;

public class S3TraceQueryProcessorTest {

  private static ExportTraceServiceRequest req(
      String traceHex, String spanHex, long sMs, long eMs) {
    Span sp =
        Span.newBuilder()
            .setTraceId(ByteString.copyFrom(java.util.HexFormat.of().parseHex(traceHex)))
            .setSpanId(ByteString.copyFrom(java.util.HexFormat.of().parseHex(spanHex)))
            .setStartTimeUnixNano(sMs * 1_000_000L)
            .setEndTimeUnixNano(eMs * 1_000_000L)
            .build();
    ScopeSpans scope = ScopeSpans.newBuilder().addSpans(sp).build();
    ResourceSpans rs = ResourceSpans.newBuilder().addScopeSpans(scope).build();
    return ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
  }

  private static byte[] fileWithPages(byte[]... pages) throws Exception {
    int total = 0;
    for (byte[] p : pages) total += p.length;
    byte[] all = new byte[total];
    int pos = 0;
    for (byte[] p : pages) {
      System.arraycopy(p, 0, all, pos, p.length);
      pos += p.length;
    }
    return all;
  }

  private static class BytesFetcher implements S3TraceQueryProcessor.RangeFetcher {
    private final byte[] file;
    boolean failHeader;

    BytesFetcher(byte[] file) {
      this.file = file;
    }

    @Override
    public byte[] getRange(String b, String k, long s, long e) {
      if (failHeader) throw new RuntimeException("fetch failed");
      if (s >= file.length) return null;
      int to = (int) Math.min(file.length, e);
      int from = (int) Math.max(0, s);
      if (from >= to) return null;
      byte[] out = new byte[to - from];
      System.arraycopy(file, from, out, 0, out.length);
      return out;
    }
  }

  private static class DummyResolver implements S3TracefileKeyResolver {
    @Override
    public String bucket() {
      return "b";
    }

    @Override
    public String keyFor(String t, String a, long hb) {
      return t + "/" + a + "/" + hb;
    }
  }

  private static class CountingMetrics implements org.okapi.traces.metrics.MetricsEmitter {
    long parseErr;
    long pageRead;
    long bloomMiss;
    long bloomHit;
    long spans;

    @Override
    public void emitPageRead(String t, String a) {
      pageRead++;
    }

    @Override
    public void emitPageTimeSkipped(String t, String a) {}

    @Override
    public void emitBloomChecked(String t, String a) {}

    @Override
    public void emitBloomHit(String t, String a) {
      bloomHit++;
    }

    @Override
    public void emitBloomMiss(String t, String a) {
      bloomMiss++;
    }

    @Override
    public void emitPageParsed(String t, String a) {}

    @Override
    public void emitPageParseError(String t, String a) {
      parseErr++;
    }

    @Override
    public void emitSpansMatched(String t, String a, long c) {
      spans += c;
    }
  }

  @Test
  void getSpans_WithFilter_happy_bloomMiss_crcFail_and_timeSkip() throws Exception {
    long base = 1700000000000L;
    // Page with 1 span trace A and 1 span trace B
    SpanPage page = SpanPage.newEmpty(1000, 0.01);
    page.append(req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", base + 10, base + 20));
    page.append(req("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "2222222222222222", base + 30, base + 40));
    byte[] p1 = page.serialize();

    // Page outside time window
    SpanPage page2 = SpanPage.newEmpty(1000, 0.01);
    page2.append(
        req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "3333333333333333", base + 10_000, base + 10_100));
    byte[] p2 = page2.serialize();

    byte[] file = fileWithPages(p1, p2);
    BytesFetcher fetcher = new BytesFetcher(file);
    CountingMetrics metrics = new CountingMetrics();
    TraceQueryConfig cfg = TraceQueryConfig.builder().queryThreads(1).build();
    S3TraceQueryProcessor s3 = new S3TraceQueryProcessor(fetcher, new DummyResolver(), cfg, metrics);

    // Happy traceId
    List<Span> spans =
        s3.getSpansWithFilter(base, base + 1000, "t", "a", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    assertEquals(1, spans.size());
    assertTrue(metrics.pageRead >= 1);
    assertTrue(metrics.bloomHit >= 1);

    // Bloom miss (unknown trace)
    CountingMetrics metrics2 = new CountingMetrics();
    S3TraceQueryProcessor s3b = new S3TraceQueryProcessor(fetcher, new DummyResolver(), cfg, metrics2);
    List<Span> none =
        s3b.getSpansWithFilter(base, base + 1000, "t", "a", "cccccccccccccccccccccccccccccccc");
    assertTrue(none.isEmpty());
    assertTrue(metrics2.bloomMiss >= 1);

    // CRC fail by corrupting inner payload bytes
    byte[] corrupt = file.clone();
    int flip = 12; // within first page inner
    corrupt[flip] ^= 0x01;
    BytesFetcher bad = new BytesFetcher(corrupt);
    CountingMetrics metrics3 = new CountingMetrics();
    S3TraceQueryProcessor s3c = new S3TraceQueryProcessor(bad, new DummyResolver(), cfg, metrics3);
    List<Span> empty =
        s3c.getSpansWithFilter(base, base + 1000, "t", "a", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    assertTrue(empty.isEmpty());
    assertTrue(metrics3.parseErr >= 1);

    // Time window skip: query far ahead
    CountingMetrics metrics4 = new CountingMetrics();
    S3TraceQueryProcessor s3d = new S3TraceQueryProcessor(fetcher, new DummyResolver(), cfg, metrics4);
    List<Span> empty2 =
        s3d.getSpansWithFilter(
            base + 5000, base + 6000, "t", "a", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    assertTrue(empty2.isEmpty());
  }

  @Test
  void getSpans_byAttribute_and_getTraceWithFilter() throws Exception {
    long base = 1700003600000L;
    // Build two spans of same trace
    SpanPage page = SpanPage.newEmpty(1000, 0.01);
    page.append(req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", base + 10, base + 20));
    page.append(req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "2222222222222222", base + 30, base + 40));
    byte[] file = fileWithPages(page.serialize());
    BytesFetcher fetcher = new BytesFetcher(file);
    TraceQueryConfig cfg = TraceQueryConfig.builder().queryThreads(1).build();
    S3TraceQueryProcessor s3 =
        new S3TraceQueryProcessor(fetcher, new DummyResolver(), cfg, new CountingMetrics());

    var attr =
        AttributeFilter.withPattern("service.name", ".*"); // no resource attrs in test; expect 0
    List<Span> attrSpans = s3.getSpansWithFilter(base, base + 1000, "t", "a", attr);
    assertTrue(attrSpans.isEmpty());

    List<Span> chain = s3.getTrace(base, base + 1000, "t", "a", "1111111111111111");
    // returns all spans for the trace
    assertEquals(2, chain.size());
  }
}
