package org.okapi.logs.testutil;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;

/** Reusable test corpora for OTel ingestion tests. */
public final class OtelTestPayloads {
  public static final String TRACE_A_HEX = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  public static final String TRACE_B_HEX = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
  public static final byte[] TRACE_A_BYTES = new byte[] {(byte) 0xAA, (byte) 0xAA};
  public static final byte[] TRACE_B_BYTES = new byte[] {(byte) 0xBB, (byte) 0xBB};
  private OtelTestPayloads() {}

  public static Result traceCheckoutScenario(long baseTsMillis) {
    var baseNs = baseTsMillis * 1_000_000L;

    var req = ExportTraceServiceRequest.newBuilder();
    var rs = ResourceSpans.newBuilder();
    var ss = ScopeSpans.newBuilder();
    ss.addSpans(
        makeSpan(
            TRACE_A_BYTES,
            baseNs + 1,
            baseNs + 9,
            Map.of(
                "http.method",
                "GET",
                "http.url",
                "/api/catalog/search?q=running+shoes",
                "user",
                "u123")));

    ss.addSpans(
        makeSpan(
            TRACE_A_BYTES,
            baseNs + 2,
            baseNs + 5,
            Map.of(
                "component",
                "PricingEngine",
                "action",
                "applied discount",
                "code",
                "SPRING10",
                "order",
                "1001")));

    ss.addSpans(
        makeSpan(
            TRACE_A_BYTES,
            baseNs + 3,
            baseNs + 13,
            Map.of("event", "Inventory low", "sku", "SHOE-RED-42", "remaining", "2")));

    ss.addSpans(
        makeSpan(
            TRACE_A_BYTES,
            baseNs + 4,
            baseNs + 11,
            Map.of(
                "event",
                "Payment authorized",
                "order",
                "1001",
                "amount",
                "79.99",
                "provider",
                "stripe")));

    ss.addSpans(
        makeSpan(
            TRACE_A_BYTES,
            baseNs + 5,
            baseNs + 9,
            Map.of("event", "Order confirmed", "order", "1001", "user", "u123")));
    // trace -b should now fail

    ss.addSpans(
        makeSpan(
            TRACE_B_BYTES,
            baseNs + 6,
            baseNs + 9,
            Map.of(
                "http.method",
                "GET",
                "http.url",
                "/api/catalog/product/SHOE-BLUE-41",
                "user",
                "u999")));
    ss.addSpans(
        makeSpan(
            TRACE_B_BYTES,
            baseNs + 7,
            baseNs + 17,
            Map.of(
                "event",
                "Payment authorization failed",
                "order",
                "2002",
                "amount",
                "129.00",
                "provider",
                "stripe",
                "code",
                "card_declined")));
    rs.addScopeSpans(ss);
    var request = req.addResourceSpans(rs.build()).build();
    return new Result(request.toByteArray(), baseTsMillis);
  }

  public static Span makeSpan(
      byte[] traceId, long nanoSt, long nanoEn, Map<String, String> attributes) {
    var kvs =
        attributes.entrySet().stream()
            .map(
                e ->
                    KeyValue.newBuilder()
                        .setKey(e.getKey())
                        .setValue(
                            io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                                .setStringValue(e.getValue())
                                .build())
                        .build())
            .toList();
    var spanId = RandomStringUtils.secure().next(8, true, false);
    return Span.newBuilder()
        .setSpanId(ByteString.copyFrom(spanId.getBytes()))
        .setStartTimeUnixNano(nanoSt)
        .setEndTimeUnixNano(nanoEn)
        .setTraceId(ByteString.copyFrom(traceId))
        .addAllAttributes(kvs)
        .build();
  }

  /**
   * Builds a 10-record payload across two traces with predictable content: - Trace A: 5 records;
   * includes one WARN. - Trace B: 5 records; includes two ERRORs and one WARN; two messages contain
   * the word "failed".
   *
   * @param baseTsMillis base timestamp in milliseconds to construct records near this time
   * @return a result containing serialized request bytes and the first record ts in millis
   */
  public static Result checkoutScenario(long baseTsMillis) {
    long baseNs = baseTsMillis * 1_000_000L;
    byte[] traceA = new byte[16];
    byte[] traceB = new byte[16];
    java.util.Arrays.fill(traceA, (byte) 0xAA);
    java.util.Arrays.fill(traceB, (byte) 0xBB);

    var req = ExportLogsServiceRequest.newBuilder();
    var rl = ResourceLogs.newBuilder();
    var sl = ScopeLogs.newBuilder();

    // Trace A (successful checkout)
    sl.addLogRecords(
        log(baseNs + 1, 9, traceA, "GET /api/catalog/search?q=running+shoes user=u123")); // INFO
    sl.addLogRecords(
        log(
            baseNs + 2,
            5,
            traceA,
            "PricingEngine applied discount code=SPRING10 order=1001")); // DEBUG
    sl.addLogRecords(
        log(baseNs + 3, 13, traceA, "Inventory low for sku=SHOE-RED-42 remaining=2")); // WARN
    sl.addLogRecords(
        log(
            baseNs + 4,
            11,
            traceA,
            "Payment authorized order=1001 amount=79.99 provider=stripe")); // INFO
    sl.addLogRecords(log(baseNs + 5, 9, traceA, "Order confirmed order=1001 user=u123")); // INFO

    // Trace B (failed checkout)
    sl.addLogRecords(
        log(baseNs + 6, 9, traceB, "GET /api/catalog/product/SHOE-BLUE-41 user=u999")); // INFO
    sl.addLogRecords(
        log(
            baseNs + 7,
            17,
            traceB,
            "Payment authorization failed order=2002 amount=129.00 provider=stripe code=card_declined")); // ERROR
    sl.addLogRecords(log(baseNs + 8, 13, traceB, "Cart abandoned user=u999 after=2m")); // WARN
    sl.addLogRecords(
        log(
            baseNs + 9,
            17,
            traceB,
            "Order creation failed order=2002 cause=payment_error")); // ERROR
    sl.addLogRecords(
        log(baseNs + 10, 9, traceB, "Retry scheduled for payment order=2002 in=5m")); // INFO

    rl.addScopeLogs(sl);
    req.addResourceLogs(rl);

    return new Result(req.build().toByteArray(), baseTsMillis);
  }

  private static LogRecord log(
      long timeUnixNano, int severityNumber, byte[] traceIdBytes, String body) {
    return LogRecord.newBuilder()
        .setTimeUnixNano(timeUnixNano)
        .setSeverityNumber(SeverityNumber.forNumber(severityNumber))
        .setTraceId(ByteString.copyFrom(traceIdBytes))
        .setBody(
            io.opentelemetry.proto.common.v1.AnyValue.newBuilder().setStringValue(body).build())
        .build();
  }

  public record Result(byte[] body, long startTsMillis) {}
}
