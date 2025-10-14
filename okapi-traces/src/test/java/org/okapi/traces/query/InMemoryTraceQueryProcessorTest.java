package org.okapi.traces.query;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.traces.page.BufferPoolManager;
import org.okapi.traces.page.LogAndDropWriteFailedListener;
import org.okapi.traces.page.SizeBasedFlushStrategy;
import org.okapi.traces.page.TraceFileWriter;

public class InMemoryTraceQueryProcessorTest {

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
  void query_unflushed_pages_byTrace_and_attribute_and_getTrace() throws Exception {
    var writer = new TraceFileWriter(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"))); // not used
    var bpm = new BufferPoolManager(new SizeBasedFlushStrategy(Long.MAX_VALUE), writer, new LogAndDropWriteFailedListener(), 1000, 0.01);

    String tenant = "t"; String app = "a";
    long base = 1700000000000L;
    bpm.consume(tenant, app, makePayload("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", null, base+10, base+20));
    bpm.consume(tenant, app, makePayload("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "2222222222222222", "1111111111111111", base+30, base+40));
    var kv2 = KeyValue.newBuilder().setKey("service.name").setValue(AnyValue.newBuilder().setStringValue("payments")).build();
    bpm.consume(tenant, app, makePayload("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "3333333333333333", null, base+50, base+60, kv2));

    var proc = new InMemoryTraceQueryProcessor(bpm);
    var spans = proc.getSpans(base, base+1000, tenant, app, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    assertEquals(2, spans.size());

    var patt = AttributeFilter.withPattern("service.name", "pay.*");
    var spans2 = proc.getSpans(base, base+1000, tenant, app, patt);
    assertEquals(1, spans2.size());

    var chain = proc.getTrace(base, base+1000, tenant, app, "3333333333333333");
    // only original; parents may not be present or belong to other trace
    assertTrue(chain.size() >= 1);
  }
}

