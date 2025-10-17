package org.okapi.traces.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.jupiter.api.Test;
import org.okapi.traces.page.TraceBufferPoolManager;

public class TraceServiceTest {

  @Test
  void ingest_counts_spans_and_calls_consume() throws Exception {
    TraceBufferPoolManager bpm = mock(TraceBufferPoolManager.class);
    TraceService svc = new TraceService(bpm);

    long now = 1700000000000L;
    var s1 = Span.newBuilder()
        .setTraceId(ByteString.copyFrom(new byte[16]))
        .setSpanId(ByteString.copyFrom(new byte[8]))
        .setStartTimeUnixNano(now*1_000_000L)
        .setEndTimeUnixNano((now+1)*1_000_000L)
        .build();
    var s2 = s1.toBuilder().setSpanId(ByteString.copyFrom(new byte[]{1,2,3,4,5,6,7,8})).build();
    var scope = ScopeSpans.newBuilder().addSpans(s1).addSpans(s2).build();
    var rs = ResourceSpans.newBuilder().addScopeSpans(scope).build();
    var req = ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
    int count = svc.ingestOtelProtobuf(req.toByteArray(), "ten", "app");
    assertEquals(2, count);
    verify(bpm, times(1)).consume(eq("ten"), eq("app"), any());
  }

  @Test
  void ingest_invalid_bytes_returns_zero() {
    TraceBufferPoolManager bpm = mock(TraceBufferPoolManager.class);
    TraceService svc = new TraceService(bpm);
    int count = svc.ingestOtelProtobuf(new byte[]{1,2,3}, "t", "a");
    assertEquals(0, count);
    verify(bpm, never()).consume(any(), any(), any());
  }
}

