package org.okapi.traces.query;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MultiplexingTraceQueryProcessorTest {

  private static byte[] hex(String h){ return HexFormat.of().parseHex(h); }

  private static Span mk(String traceId, String spanId, long startMs) {
    return Span.newBuilder()
        .setTraceId(com.google.protobuf.ByteString.copyFrom(hex(traceId)))
        .setSpanId(com.google.protobuf.ByteString.copyFrom(hex(spanId)))
        .setStartTimeUnixNano(startMs * 1_000_000L)
        .setEndTimeUnixNano((startMs+10) * 1_000_000L)
        .build();
  }

  static class StubProc implements TraceQueryProcessor {
    List<Span> a; List<Span> b; List<Span> t;
    StubProc(List<Span> a, List<Span> b, List<Span> t) { this.a=a; this.b=b; this.t=t; }
    @Override public List<Span> getSpans(long s, long e, String ten, String app, String traceId) throws IOException { return a; }
    @Override public List<Span> getSpans(long s, long e, String ten, String app, AttributeFilter f) throws IOException { return b; }
    @Override public List<Span> getTrace(long s, long e, String ten, String app, String spanId) throws IOException { return t; }
  }

  @Test
  void merges_and_deduplicates() throws Exception {
    var s1 = mk("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "1111111111111111", 10);
    var s2 = mk("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "2222222222222222", 20);
    var s3 = mk("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "3333333333333333", 30);

    var p1 = new StubProc(List.of(s1, s2), List.of(s3), List.of(s1));
    var p2 = new StubProc(List.of(s2), List.of(s3), List.of(s1, s2));

    var mux = new MultiplexingTraceQueryProcessor(List.of(p1, p2));

    var spansA = mux.getSpans(0, 1000, "t","a","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    assertEquals(2, spansA.size());

    var spansB = mux.getSpans(0, 1000, "t","a", new AttributeFilter("x","y"));
    assertEquals(1, spansB.size());

    var chain = mux.getTrace(0, 1000, "t","a","1111111111111111");
    assertEquals(2, chain.size());
  }
}

