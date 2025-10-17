package org.okapi.traces.query;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.traces.page.LogAndDropWriteFailedListener;
import org.okapi.traces.page.SizeBasedFlushStrategy;
import org.okapi.traces.page.SpanPage;
import org.okapi.traces.page.TraceBufferPoolManager;
import org.okapi.traces.page.TraceFileWriter;
import org.okapi.traces.testutil.MockRangeFetcher;
import org.okapi.traces.testutil.TestS3Resolver;

public class MultiplexingTraceQueryProcessorSourcesTest {

  private static byte[] hex(String h) {
    return HexFormat.of().parseHex(h);
  }

  private static ExportTraceServiceRequest req(
      String traceHex, String spanHex, String parentSpanHex, long sMs, long eMs, KeyValue... attrs) {
    Span.Builder spb =
        Span.newBuilder()
            .setTraceId(ByteString.copyFrom(hex(traceHex)))
            .setSpanId(ByteString.copyFrom(hex(spanHex)))
            .setStartTimeUnixNano(sMs * 1_000_000L)
            .setEndTimeUnixNano(eMs * 1_000_000L)
            .setName("s");
    if (parentSpanHex != null && !parentSpanHex.isEmpty()) {
      spb.setParentSpanId(ByteString.copyFrom(hex(parentSpanHex)));
    }
    Span sp = spb.build();
    ScopeSpans scope = ScopeSpans.newBuilder().addSpans(sp).build();
    Resource.Builder rb = Resource.newBuilder();
    for (KeyValue kv : attrs) rb.addAttributes(kv);
    ResourceSpans rs = ResourceSpans.newBuilder().setResource(rb.build()).addScopeSpans(scope).build();
    return ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
  }

  private Path tmpDir;

  @BeforeEach
  void setup() throws Exception {
    tmpDir = Files.createTempDirectory("okapi-traces-mux-src");
  }

  @AfterEach
  void cleanup() throws Exception {
    if (tmpDir != null) {
      Files.walk(tmpDir)
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

  private InMemoryTraceQueryProcessor mkMemProc(TraceBufferPoolManager bpm) {
    return new InMemoryTraceQueryProcessor(bpm);
  }

  private FileTraceQueryProcessor mkFileProc(Path base) {
    return new FileTraceQueryProcessor(base, TraceQueryConfig.builder().queryThreads(1).build());
  }

  private S3TraceQueryProcessor mkS3Proc(MockRangeFetcher fetcher, String bucket) {
    TraceQueryConfig cfg = TraceQueryConfig.builder().queryThreads(1).build();
    return new S3TraceQueryProcessor(fetcher, new TestS3Resolver(bucket), cfg, null);
  }

  private TraceBufferPoolManager mkBufferPool(Path writerDir) {
    return new TraceBufferPoolManager(
        new SizeBasedFlushStrategy(Long.MAX_VALUE),
        new TraceFileWriter(writerDir),
        new LogAndDropWriteFailedListener(),
        1000,
        0.01);
  }

  @Test
  void traceId_merge_and_dedup_across_sources() throws Exception {
    String tenant = "t";
    String app = "a";
    long base = 1700000000000L;
    long hb = base / 3_600_000L;

    // In-memory: s1
    var bpm = mkBufferPool(tmpDir);
    var mem = mkMemProc(bpm);
    bpm.consume(tenant, app, req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", null, base + 10, base + 20));

    // File: s2
    var writer = new TraceFileWriter(tmpDir);
    var page = SpanPage.newEmpty(1000, 0.01);
    page.append(req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "2222222222222222", null, base + 30, base + 40));
    writer.write(tenant, app, page);
    var file = mkFileProc(tmpDir);

    // S3: s2 duplicate
    var s3fetch = new MockRangeFetcher();
    var s3 = mkS3Proc(s3fetch, "b");
    var s3page = SpanPage.newEmpty(1000, 0.01);
    s3page.append(req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "2222222222222222", null, base + 30, base + 40));
    byte[] s3file = s3page.serialize();
    s3fetch.putFile("b", new TestS3Resolver("b").keyFor(tenant, app, hb), s3file);

    var mux = new MultiplexingTraceQueryProcessor(List.of(mem, file, s3));
    try {
      var spans = mux.getSpansWithFilter(base, base + 1000, tenant, app, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
      assertEquals(2, spans.size());
      assertEquals("1111111111111111", HexFormat.of().formatHex(spans.get(0).getSpanId().toByteArray()));
      assertEquals("2222222222222222", HexFormat.of().formatHex(spans.get(1).getSpanId().toByteArray()));
    } finally {
      mux.close();
    }
  }

  @Test
  void attribute_filter_aggregates_across_sources() throws Exception {
    String tenant = "t";
    String app = "a";
    long base = 1700000000000L;
    long hb = base / 3_600_000L;
    var attr = KeyValue.newBuilder().setKey("service.name").setValue(AnyValue.newBuilder().setStringValue("orders")).build();

    // Memory: s1 with attr
    var bpm = mkBufferPool(tmpDir);
    bpm.consume(tenant, app, req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", null, base + 10, base + 20, attr));
    var mem = mkMemProc(bpm);

    // File: s2 with attr
    var writer = new TraceFileWriter(tmpDir);
    var page = SpanPage.newEmpty(1000, 0.01);
    page.append(req("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "2222222222222222", null, base + 30, base + 40, attr));
    writer.write(tenant, app, page);
    var file = mkFileProc(tmpDir);

    // S3: unrelated span without matching attr
    var s3fetch = new MockRangeFetcher();
    var s3 = mkS3Proc(s3fetch, "b");
    var s3page = SpanPage.newEmpty(1000, 0.01);
    s3page.append(req("cccccccccccccccccccccccccccccccc", "3333333333333333", null, base + 50, base + 60));
    s3fetch.putFile("b", new TestS3Resolver("b").keyFor(tenant, app, hb), s3page.serialize());

    var mux = new MultiplexingTraceQueryProcessor(List.of(mem, file, s3));
    try {
      var filter = AttributeFilter.withPattern("service.name", "ord.*");
      var spans = mux.getSpansWithFilter(base, base + 1000, tenant, app, filter);
      assertEquals(2, spans.size());
      var ids = spans.stream().map(s -> HexFormat.of().formatHex(s.getSpanId().toByteArray())).toList();
      assertTrue(ids.containsAll(List.of("1111111111111111", "2222222222222222")));
    } finally {
      mux.close();
    }
  }

  @Test
  void error_isolation_when_one_source_fails() throws Exception {
    String tenant = "t";
    String app = "a";
    long base = 1700000000000L;
    long hb = base / 3_600_000L;

    // Memory
    var bpm = mkBufferPool(tmpDir);
    bpm.consume(tenant, app, req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", null, base + 10, base + 20));
    var mem = mkMemProc(bpm);

    // File
    var writer = new TraceFileWriter(tmpDir);
    var page = SpanPage.newEmpty(1000, 0.01);
    page.append(req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "2222222222222222", null, base + 30, base + 40));
    writer.write(tenant, app, page);
    var file = mkFileProc(tmpDir);

    // S3: configured to fail
    var s3fetch = new MockRangeFetcher();
    var resolver = new TestS3Resolver("b");
    s3fetch.putFile("b", resolver.keyFor(tenant, app, hb), page.serialize());
    s3fetch.setFail("b", resolver.keyFor(tenant, app, hb), true);
    var s3 = mkS3Proc(s3fetch, "b");

    var mux = new MultiplexingTraceQueryProcessor(List.of(mem, file, s3));
    try {
      var spans = mux.getSpansWithFilter(base, base + 1000, tenant, app, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
      // Should still get from mem + file, not throw
      assertEquals(2, spans.size());
    } finally {
      mux.close();
    }
  }

  @Test
  void getTrace_target_only_in_memory_documents_current_limitation() throws Exception {
    String tenant = "t";
    String app = "a";
    long base = 1700000000000L;

    // Memory: only target span c2
    var bpm = mkBufferPool(tmpDir);
    bpm.consume(tenant, app, req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "3333333333333333", "2222222222222222", base + 50, base + 60));
    var mem = mkMemProc(bpm);

    // File: only parent spans (without the target)
    var writer = new TraceFileWriter(tmpDir);
    var page = SpanPage.newEmpty(1000, 0.01);
    page.append(req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", null, base + 10, base + 20));
    page.append(req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "2222222222222222", "1111111111111111", base + 30, base + 40));
    writer.write(tenant, app, page);
    var file = mkFileProc(tmpDir);

    // No S3
    var mux = new MultiplexingTraceQueryProcessor(List.of(mem, file));
    try {
      var chain = mux.getTrace(base, base + 1000, tenant, app, "3333333333333333");
      // Current behavior: only the source that finds target constructs chain; others return empty
      assertEquals(1, chain.size());
      assertEquals("3333333333333333", HexFormat.of().formatHex(chain.get(0).getSpanId().toByteArray()));
    } finally {
      mux.close();
    }
  }

  @Test
  void getTrace_duplicate_target_across_sources_includes_parent() throws Exception {
    String tenant = "t";
    String app = "a";
    long base = 1700000000000L;
    long hb = base / 3_600_000L;

    // Memory: target c1 only
    var bpm = mkBufferPool(tmpDir);
    bpm.consume(tenant, app, req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "2222222222222222", "1111111111111111", base + 30, base + 40));
    var mem = mkMemProc(bpm);

    // S3: target + parent in same page
    var s3fetch = new MockRangeFetcher();
    var s3page = SpanPage.newEmpty(1000, 0.01);
    s3page.append(req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", null, base + 10, base + 20));
    s3page.append(req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "2222222222222222", "1111111111111111", base + 30, base + 40));
    s3fetch.putFile("b", new TestS3Resolver("b").keyFor(tenant, app, hb), s3page.serialize());
    var s3 = mkS3Proc(s3fetch, "b");

    var mux = new MultiplexingTraceQueryProcessor(List.of(mem, s3));
    try {
      var chain = mux.getTrace(base, base + 1000, tenant, app, "2222222222222222");
      // Expect both parent and target after merging chains (dedup will remove duplicate target)
      assertEquals(2, chain.size());
      var ids = chain.stream().map(s -> HexFormat.of().formatHex(s.getSpanId().toByteArray())).toList();
      assertTrue(ids.containsAll(List.of("1111111111111111", "2222222222222222")));
    } finally {
      mux.close();
    }
  }

  @Test
  void bloom_miss_does_not_mask_other_sources() throws Exception {
    String tenant = "t";
    String app = "a";
    long base = 1700000000000L;
    long hb = base / 3_600_000L;

    // Memory has the trace
    var bpm = mkBufferPool(tmpDir);
    bpm.consume(tenant, app, req("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", null, base + 10, base + 20));
    var mem = mkMemProc(bpm);

    // S3 has a different trace; bloom will miss the target trace
    var s3fetch = new MockRangeFetcher();
    var s3page = SpanPage.newEmpty(1000, 0.01);
    s3page.append(req("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "2222222222222222", null, base + 30, base + 40));
    s3fetch.putFile("b", new TestS3Resolver("b").keyFor(tenant, app, hb), s3page.serialize());
    var s3 = mkS3Proc(s3fetch, "b");

    var mux = new MultiplexingTraceQueryProcessor(List.of(s3, mem)); // S3 first to prove miss doesn't block others
    try {
      var spans = mux.getSpansWithFilter(base, base + 1000, tenant, app, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
      assertEquals(1, spans.size());
      assertEquals("1111111111111111", HexFormat.of().formatHex(spans.get(0).getSpanId().toByteArray()));
    } finally {
      mux.close();
    }
  }
}

