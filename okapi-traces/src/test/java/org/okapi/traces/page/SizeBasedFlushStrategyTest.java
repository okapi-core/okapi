package org.okapi.traces.page;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.jupiter.api.Test;

public class SizeBasedFlushStrategyTest {

  private static ExportTraceServiceRequest req() {
    long now = 1700000000000L;
    var sp =
        Span.newBuilder()
            .setTraceId(com.google.protobuf.ByteString.copyFrom(new byte[16]))
            .setSpanId(com.google.protobuf.ByteString.copyFrom(new byte[8]))
            .setStartTimeUnixNano(now * 1_000_000L)
            .setEndTimeUnixNano((now + 1) * 1_000_000L)
            .build();
    var scope = ScopeSpans.newBuilder().addSpans(sp).build();
    var rs = ResourceSpans.newBuilder().addScopeSpans(scope).build();
    return ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
  }

  @Test
  void shouldFlush_threshold_boundaries() {
    var page = SpanPage.newEmpty(1000, 0.01);
    page.append(req());
    long est = page.estimatedSerializedSizeBytes();
    SizeBasedFlushStrategy sLow = new SizeBasedFlushStrategy(est + 100);
    SizeBasedFlushStrategy sHigh = new SizeBasedFlushStrategy(Math.max(1, est - 1));
    assertFalse(sLow.shouldFlush(page));
    assertTrue(sHigh.shouldFlush(page));
  }
}
