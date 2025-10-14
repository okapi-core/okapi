package org.okapi.traces.query;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.traces.page.SpanPage;
import org.okapi.traces.page.TraceFileWriter;

public class TraceQueryServiceTest {

  Path dir;

  @BeforeEach
  void setup() throws Exception { dir = Files.createTempDirectory("okapi-trace-query"); }

  @AfterEach
  void cleanup() throws Exception {
    if (dir != null) {
      Files.walk(dir).sorted((a,b)->b.getNameCount()-a.getNameCount()).forEach(p -> { try { Files.deleteIfExists(p);} catch(Exception ignored) {} });
    }
  }

  private static byte[] hex(String h){ return HexFormat.of().parseHex(h); }

  private static ExportTraceServiceRequest makePayload(
      String traceId, String spanId, String parentSpanId, long startMs, long endMs, KeyValue... resAttrs) {
    Span sp = Span.newBuilder()
        .setTraceId(com.google.protobuf.ByteString.copyFrom(hex(traceId)))
        .setSpanId(com.google.protobuf.ByteString.copyFrom(hex(spanId)))
        .setParentSpanId(parentSpanId==null? com.google.protobuf.ByteString.EMPTY : com.google.protobuf.ByteString.copyFrom(hex(parentSpanId)))
        .setStartTimeUnixNano(startMs * 1_000_000L)
        .setEndTimeUnixNano(endMs * 1_000_000L)
        .setName("s")
        .build();
    ScopeSpans scope = ScopeSpans.newBuilder().addSpans(sp).build();
    Resource.Builder r = Resource.newBuilder();
    for (KeyValue kv : resAttrs) r.addAttributes(kv);
    ResourceSpans rs = ResourceSpans.newBuilder().setResource(r.build()).addScopeSpans(scope).build();
    return ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
  }

  @Test
  void getSpans_byTraceId_returnsSorted() throws Exception {
    String tenant = "t"; String app = "a";
    long base = 1700000000000L; long hb = base / 3_600_000L;
    var writer = new TraceFileWriter(dir);

    // page 1 with 2 spans of same trace
    var page1 = SpanPage.newEmpty(1000, 0.01);
    page1.append(makePayload("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", null, base+10, base+20));
    page1.append(makePayload("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "2222222222222222", "1111111111111111", base+30, base+40));
    writer.write(tenant, app, page1);

    // page 2 different trace (should be skipped by bloom)
    var page2 = SpanPage.newEmpty(1000, 0.01);
    page2.append(makePayload("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "3333333333333333", null, base+50, base+60));
    writer.write(tenant, app, page2);

    try (var svc = new TraceQueryService(dir)) {
      List<Span> spans = svc.getSpans(base, base+1000, tenant, app, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
      assertEquals(2, spans.size());
      assertEquals("1111111111111111", HexFormat.of().formatHex(spans.get(0).getSpanId().toByteArray()));
      assertEquals("2222222222222222", HexFormat.of().formatHex(spans.get(1).getSpanId().toByteArray()));
    }
  }

  @Test
  void getSpans_byAttributeFilter_exact_and_pattern() throws Exception {
    String tenant = "t"; String app = "a";
    long base = 1700003600000L;
    var writer = new TraceFileWriter(dir);

    var kv1 = KeyValue.newBuilder().setKey("service.name").setValue(AnyValue.newBuilder().setStringValue("orders")).build();
    var kv2 = KeyValue.newBuilder().setKey("service.name").setValue(AnyValue.newBuilder().setStringValue("payments")).build();

    var page = SpanPage.newEmpty(1000, 0.01);
    page.append(makePayload("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", null, base+10, base+20, kv1));
    page.append(makePayload("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "2222222222222222", null, base+30, base+40, kv2));
    writer.write(tenant, app, page);

    try (var svc = new TraceQueryService(dir)) {
      var exact = new AttributeFilter("service.name", "orders");
      var spans1 = svc.getSpans(base, base+1000, tenant, app, exact);
      assertEquals(1, spans1.size());

      var patt = AttributeFilter.withPattern("service.name", "pay.*");
      var spans2 = svc.getSpans(base, base+1000, tenant, app, patt);
      assertEquals(1, spans2.size());
    }
  }

  @Test
  void getTrace_includes_original_and_parents() throws Exception {
    String tenant = "t"; String app = "a";
    long base = 1700010000000L;
    var writer = new TraceFileWriter(dir);

    String trace = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    String p1 = "1111111111111111"; // root
    String c1 = "2222222222222222"; // child of p1
    String c2 = "3333333333333333"; // child of c1
    var page = SpanPage.newEmpty(1000, 0.01);
    page.append(makePayload(trace, p1, null, base+10, base+20));
    page.append(makePayload(trace, c1, p1, base+30, base+40));
    page.append(makePayload(trace, c2, c1, base+50, base+60));
    writer.write(tenant, app, page);

    try (var svc = new TraceQueryService(dir)) {
      var chain = svc.getTrace(base, base+1000, tenant, app, c2);
      assertEquals(3, chain.size()); // includes original and parents
      // contains c2, c1, p1 (order by startTime after sorting)
      var ids = chain.stream().map(s -> HexFormat.of().formatHex(s.getSpanId().toByteArray())).toList();
      assertTrue(ids.containsAll(List.of(p1, c1, c2)));
    }
  }
}

