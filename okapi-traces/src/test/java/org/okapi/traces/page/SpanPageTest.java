package org.okapi.traces.page;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

public class SpanPageTest {

  private static byte[] hexToBytes(String hex) {
    return HexFormat.of().parseHex(hex);
  }

  private static ExportTraceServiceRequest oneSpanPayload(
      String traceIdHex, String spanIdHex, long startMillis, long endMillis) {
    var span =
        io.opentelemetry.proto.trace.v1.Span.newBuilder()
            .setTraceId(com.google.protobuf.ByteString.copyFrom(hexToBytes(traceIdHex)))
            .setSpanId(com.google.protobuf.ByteString.copyFrom(hexToBytes(spanIdHex)))
            .setName("span")
            .setStartTimeUnixNano(startMillis * 1_000_000L)
            .setEndTimeUnixNano(endMillis * 1_000_000L)
            .build();
    var scope = io.opentelemetry.proto.trace.v1.ScopeSpans.newBuilder().addSpans(span).build();
    var rs =
        io.opentelemetry.proto.trace.v1.ResourceSpans.newBuilder().addScopeSpans(scope).build();
    return ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
  }

  @Test
  void roundTrip_singlePayload_preservesWindowAndPayloads() throws Exception {
    long start = 1700000000000L;
    long end = start + 2500L;
    var payload =
        oneSpanPayload(
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbb", start, end);

    var page = SpanPage.newEmpty(1000, 0.01);
    page.append(payload);

    assertEquals(start, page.getTsStartMillis());
    assertEquals(end, page.getTsEndMillis());
    assertEquals(1, page.getPayloadCount());
    assertEquals(1, page.getSpanCount());

    byte[] bytes = page.serialize();
    var decoded = SpanPage.deserialize(bytes);

    assertEquals(start, decoded.getTsStartMillis());
    assertEquals(end, decoded.getTsEndMillis());
    assertEquals(1, decoded.getPayloadCount());
    assertEquals(1, decoded.getSpanCount());

    // Bloom filter should likely contain the trace id
    assertTrue(decoded.getTraceBloomFilter().mightContain("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
  }

  @Test
  void roundTrip_multiplePayloads_aggregatesWindow() throws Exception {
    long t0 = 1700000100000L;
    long t1 = t0 + 10_000L;
    long t2 = t0 + 20_000L;
    var p1 = oneSpanPayload("11111111111111111111111111111111", "2222222222222222", t0, t1);
    var p2 = oneSpanPayload("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbb", t1, t2);

    var page = SpanPage.newEmpty(1000, 0.01);
    page.append(p1);
    page.append(p2);
    assertEquals(t0, page.getTsStartMillis());
    assertEquals(t2, page.getTsEndMillis());
    assertEquals(2, page.getPayloadCount());
    assertEquals(2, page.getSpanCount());

    var decoded = SpanPage.deserialize(page.serialize());
    assertEquals(t0, decoded.getTsStartMillis());
    assertEquals(t2, decoded.getTsEndMillis());
    assertEquals(2, decoded.getPayloadCount());
    assertEquals(2, decoded.getSpanCount());
  }

  @Test
  void tamper_crc_mismatch_throws() throws Exception {
    var p = oneSpanPayload("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbb", 1, 2);
    var page = SpanPage.newEmpty(10, 0.01);
    page.append(p);
    byte[] bytes = page.serialize();
    // Flip a byte in the inner payload (after 8+8+4 header + some)
    int idx = 12; // within the payload length+crc header block
    bytes[idx] = (byte) (bytes[idx] ^ 0x01);
    assertThrows(Exception.class, () -> SpanPage.deserialize(bytes));
  }
}
