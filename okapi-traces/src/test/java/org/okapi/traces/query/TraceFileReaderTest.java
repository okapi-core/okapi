package org.okapi.traces.query;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.traces.metrics.MetricsEmitter;
import org.okapi.traces.page.SpanPage;

public class TraceFileReaderTest {

  private Path dir;

  @BeforeEach
  void setup() throws Exception {
    dir = Files.createTempDirectory("okapi-trace-reader");
  }

  @AfterEach
  void cleanup() throws Exception {
    if (dir != null) {
      Files.walk(dir)
          .sorted((a, b) -> b.getNameCount() - a.getNameCount())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
              });
    }
  }

  private static ExportTraceServiceRequest makeReq(
      String traceIdHex, String spanIdHex, long startMs, long endMs) {
    Span sp =
        Span.newBuilder()
            .setTraceId(ByteString.copyFrom(hex(traceIdHex)))
            .setSpanId(ByteString.copyFrom(hex(spanIdHex)))
            .setStartTimeUnixNano(startMs * 1_000_000L)
            .setEndTimeUnixNano(endMs * 1_000_000L)
            .build();
    ScopeSpans scope = ScopeSpans.newBuilder().addSpans(sp).build();
    ResourceSpans rs = ResourceSpans.newBuilder().addScopeSpans(scope).build();
    return ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
  }

  private static byte[] hex(String h) {
    return java.util.HexFormat.of().parseHex(h);
  }

  private static class CapturingMetrics implements MetricsEmitter {
    AtomicInteger pageRead = new AtomicInteger();
    AtomicInteger pageTimeSkipped = new AtomicInteger();
    AtomicInteger bloomChecked = new AtomicInteger();
    AtomicInteger bloomHit = new AtomicInteger();
    AtomicInteger bloomMiss = new AtomicInteger();
    AtomicInteger pageParsed = new AtomicInteger();
    AtomicInteger pageParseError = new AtomicInteger();
    AtomicInteger spansMatched = new AtomicInteger();

    @Override
    public void emitPageRead(String t, String a) {
      pageRead.incrementAndGet();
    }

    @Override
    public void emitPageTimeSkipped(String t, String a) {
      pageTimeSkipped.incrementAndGet();
    }

    @Override
    public void emitBloomChecked(String t, String a) {
      bloomChecked.incrementAndGet();
    }

    @Override
    public void emitBloomHit(String t, String a) {
      bloomHit.incrementAndGet();
    }

    @Override
    public void emitBloomMiss(String t, String a) {
      bloomMiss.incrementAndGet();
    }

    @Override
    public void emitPageParsed(String t, String a) {
      pageParsed.incrementAndGet();
    }

    @Override
    public void emitPageParseError(String t, String a) {
      pageParseError.incrementAndGet();
    }

    @Override
    public void emitSpansMatched(String t, String a, long c) {
      spansMatched.addAndGet((int) c);
    }
  }

  @Test
  void scanForTraceId_happy_and_branches() throws Exception {
    long base = 1700000000000L;
    // Build a page with matching trace and one unrelated trace
    SpanPage page = SpanPage.newEmpty(1000, 0.01);
    page.append(
        makeReq("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", base + 10, base + 20));
    page.append(
        makeReq("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "2222222222222222", base + 30, base + 40));
    byte[] pageBytes = page.serialize();
    Path file = dir.resolve("tracefile.bin");
    try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
      fos.write(pageBytes);
    }

    TraceFileReader reader = new TraceFileReader();
    CapturingMetrics m = new CapturingMetrics();
    List<Span> out =
        reader.scanForTraceId(
            file, base, base + 1000, "t", "a", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", m);
    assertEquals(1, out.size());
    assertTrue(m.pageRead.get() >= 1);
    assertEquals(1, m.bloomChecked.get());
    assertEquals(1, m.bloomHit.get());
    assertEquals(1, m.pageParsed.get());

    // Time-window skip
    CapturingMetrics m2 = new CapturingMetrics();
    List<Span> out2 =
        reader.scanForTraceId(
            file, base + 5000, base + 6000, "t", "a", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", m2);
    assertTrue(out2.isEmpty());
    assertEquals(1, m2.pageTimeSkipped.get());

    // Bloom miss
    CapturingMetrics m3 = new CapturingMetrics();
    List<Span> out3 =
        reader.scanForTraceId(
            file, base, base + 1000, "t", "a", "cccccccccccccccccccccccccccccccc", m3);
    assertTrue(out3.isEmpty());
    assertEquals(1, m3.bloomMiss.get());
  }

  @Test
  void crcMismatch_and_truncated_are_skipped_without_throw() throws Exception {
    long base = 1700003600000L;
    SpanPage page = SpanPage.newEmpty(1000, 0.01);
    page.append(
        makeReq("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", base + 10, base + 20));
    byte[] bytes = page.serialize();

    // Create CRC mismatch by flipping one byte in inner payload (after 8-byte header)
    byte[] tampered = bytes.clone();
    int flip = 12; // within inner payload
    if (flip < tampered.length) tampered[flip] ^= 0x01;
    Path f1 = dir.resolve("bad_crc.bin");
    Files.write(f1, tampered);

    // Create truncated page by chopping last byte
    byte[] truncated = java.util.Arrays.copyOf(bytes, bytes.length - 1);
    Path f2 = dir.resolve("trunc.bin");
    Files.write(f2, truncated);

    TraceFileReader reader = new TraceFileReader();
    CapturingMetrics m1 = new CapturingMetrics();
    List<Span> a =
        reader.scanForTraceId(
            f1, base, base + 1000, "t", "a", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", m1);
    assertTrue(a.isEmpty());
    assertTrue(m1.pageParseError.get() >= 1);

    CapturingMetrics m2 = new CapturingMetrics();
    List<Span> b =
        reader.scanForTraceId(
            f2, base, base + 1000, "t", "a", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", m2);
    assertTrue(b.isEmpty());
    // No guarantee of parseError; just ensure no exception and no spans
  }

  @Test
  void scanForAttribute_and_findSpanById() throws Exception {
    long base = 1700010000000L;
    SpanPage page = SpanPage.newEmpty(1000, 0.01);
    page.append(
        makeReq("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", base + 10, base + 20));
    byte[] pageBytes = page.serialize();
    Path file = dir.resolve("attr.bin");
    Files.write(file, pageBytes);

    TraceFileReader reader = new TraceFileReader();
    var attr = new AttributeFilter("service.name", "orders"); // won't match; returns empty
    List<Span> spans =
        reader.scanForAttributeFilter(
            file, base, base + 1000, "t", "a", attr, new CapturingMetrics());
    assertTrue(spans.isEmpty());

    var maybe =
        reader.findSpanById(file, base, base + 1000, "1111111111111111", new CancellationToken());
    assertTrue(maybe.isPresent());
  }
}
