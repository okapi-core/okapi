package org.okapi.traces;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.okapi.traces.io.SpanIngestionRecord;
import org.okapi.traces.io.SpanPage;

public class SpanPageTests {

  @Test
  void testMarkedAsFull() {
    var page = new SpanPage(200, 0.001, 10, 10);
    page.append(sampleRequest("span-1", "trace-1", "span-1"));
    page.append(sampleRequest("span-2", "trace-2", "span-2"));
    assertTrue(page.isFull(), "Page should be marked as full after exceeding thresholds");
  }

  @Test
  void testPageWithoutAppendIsMarkedEmpty() {
    var metricsPage = new SpanPage(1000, 0.05, 1000L, 1000L);
    Assertions.assertTrue(metricsPage.isEmpty());
    Assertions.assertFalse(metricsPage.isFull());
  }

  SpanIngestionRecord sampleRequest(String spanName, String traceId, String spanId) {
    var span =
        Span.newBuilder()
            .setName(spanName)
            .setTraceId(ByteString.copyFrom(traceId.getBytes()))
            .setSpanId(ByteString.copyFrom(spanId.getBytes()))
            .build();
    return SpanIngestionRecord.from("svc", span);
  }
}
