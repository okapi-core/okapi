package org.okapi.traces.page;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BufferPoolManagerTest {

  Path tmpDir;

  @BeforeEach
  void setup() throws Exception {
    tmpDir = Files.createTempDirectory("okapi-traces-test");
  }

  @AfterEach
  void cleanup() throws Exception {
    if (tmpDir != null) {
      // Best-effort cleanup
      Files.walk(tmpDir).sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
      });
    }
  }

  private static byte[] hexToBytes(String hex) { return HexFormat.of().parseHex(hex); }

  private static ExportTraceServiceRequest buildPayload(long startMillis, long endMillis) {
    var s1 =
        io.opentelemetry.proto.trace.v1.Span.newBuilder()
            .setTraceId(com.google.protobuf.ByteString.copyFrom(
                hexToBytes("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")))
            .setSpanId(com.google.protobuf.ByteString.copyFrom(hexToBytes("bbbbbbbbbbbbbbbb")))
            .setStartTimeUnixNano(startMillis * 1_000_000L)
            .setEndTimeUnixNano(endMillis * 1_000_000L)
            .setName("span")
            .build();
    var scope = io.opentelemetry.proto.trace.v1.ScopeSpans.newBuilder().addSpans(s1).build();
    var rs = io.opentelemetry.proto.trace.v1.ResourceSpans.newBuilder().addScopeSpans(scope).build();
    return ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
  }

  @Test
  void flushesOnSizeThreshold_andWritesToExpectedFile() throws Exception {
    var writer = new TraceFileWriter(tmpDir);
    var listener = new LogAndDropWriteFailedListener();
    var strategy = new SizeBasedFlushStrategy(1); // tiny threshold forces immediate flush
    var bpm = new BufferPoolManager(strategy, writer, listener, 1000, 0.01);

    long t0 = 1700000000000L;
    String tenant = "tenantA";
    String app = "appX";
    bpm.consume(tenant, app, buildPayload(t0, t0 + 100));

    long hourBucket = t0 / 3_600_000L;
    Path file = tmpDir.resolve(tenant).resolve(app).resolve("tracefile." + hourBucket + ".bin");
    assertTrue(Files.exists(file));
    assertTrue(Files.size(file) > 0);
  }

  @Test
  void writeFailure_invokesListener_andDoesNotThrow() throws Exception {
    class FailingWriter extends TraceFileWriter {
      FailingWriter() { super(Path.of("/tmp")); }
      @Override public void write(String tenantId, String application, SpanPage page) throws IOException {
        throw new IOException("disk full");
      }
    }
    var writer = new FailingWriter();
    final class Capture implements WriteFailedListener {
      int calls = 0; String lastTenant; String lastApp;
      @Override public void onWriteFaile(String tenantId, String application, SpanPage page) {
        calls++; lastTenant = tenantId; lastApp = application;
      }
    }
    var capture = new Capture();
    var strategy = new SizeBasedFlushStrategy(1);
    var bpm = new BufferPoolManager(strategy, writer, capture, 1000, 0.01);

    assertDoesNotThrow(() -> bpm.consume("t", "a", buildPayload(1, 2)));
    assertEquals(1, capture.calls);
    assertEquals("t", capture.lastTenant);
    assertEquals("a", capture.lastApp);
  }

  @Test
  void usesMinStartMillis_forHourBucket() throws Exception {
    var writer = new TraceFileWriter(tmpDir);
    var listener = new LogAndDropWriteFailedListener();
    var strategy = new SizeBasedFlushStrategy(1);
    var bpm = new BufferPoolManager(strategy, writer, listener, 1000, 0.01);

    long base = 1700003600000L; // some time slightly after an hour boundary
    long prevHour = (base - 600_000L); // 10 minutes earlier
    long nextHour = (base + 3_600_000L + 600_000L); // an hour + 10 minutes later

    // Build payload with 2 spans across hours by composing scope spans from two calls
    var s1 = io.opentelemetry.proto.trace.v1.Span.newBuilder()
        .setTraceId(com.google.protobuf.ByteString.copyFrom(hexToBytes("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")))
        .setSpanId(com.google.protobuf.ByteString.copyFrom(hexToBytes("1111111111111111")))
        .setStartTimeUnixNano(prevHour * 1_000_000L)
        .setEndTimeUnixNano((prevHour + 1000) * 1_000_000L)
        .build();
    var s2 = io.opentelemetry.proto.trace.v1.Span.newBuilder()
        .setTraceId(com.google.protobuf.ByteString.copyFrom(hexToBytes("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")))
        .setSpanId(com.google.protobuf.ByteString.copyFrom(hexToBytes("2222222222222222")))
        .setStartTimeUnixNano(nextHour * 1_000_000L)
        .setEndTimeUnixNano((nextHour + 1000) * 1_000_000L)
        .build();
    var scope = io.opentelemetry.proto.trace.v1.ScopeSpans.newBuilder().addSpans(s1).addSpans(s2).build();
    var rs = io.opentelemetry.proto.trace.v1.ResourceSpans.newBuilder().addScopeSpans(scope).build();
    var payload = ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();

    String tenant = "tenantB"; String app = "svc";
    bpm.consume(tenant, app, payload);

    long bucket = (prevHour) / 3_600_000L; // min start millis dictates bucket
    Path file = tmpDir.resolve(tenant).resolve(app).resolve("tracefile." + bucket + ".bin");
    assertTrue(Files.exists(file));
  }

  @Test
  void concurrentConsume_usesPerKeyLock_andFlushes() throws Exception {
    var writer = new TraceFileWriter(tmpDir);
    var listener = new LogAndDropWriteFailedListener();
    var strategy = new SizeBasedFlushStrategy(1); // flush each append
    var bpm = new BufferPoolManager(strategy, writer, listener, 1000, 0.01);

    String tenant = "t"; String app = "a";
    var pool = Executors.newFixedThreadPool(4);
    int n = 10;
    CountDownLatch latch = new CountDownLatch(n);
    long t0 = 1700000000000L;
    for (int i = 0; i < n; i++) {
      final int idx = i;
      pool.submit(() -> {
        try { bpm.consume(tenant, app, buildPayload(t0 + idx, t0 + idx + 5)); } finally { latch.countDown(); }
      });
    }
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    pool.shutdownNow();

    long bucket = t0 / 3_600_000L;
    Path file = tmpDir.resolve(tenant).resolve(app).resolve("tracefile." + bucket + ".bin");
    assertTrue(Files.exists(file));
    assertTrue(Files.size(file) > 0);
  }
}
