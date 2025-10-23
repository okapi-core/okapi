package org.okapi.logs.testutil;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;

/**
 * Reusable test corpora for OTel ingestion tests.
 */
public final class OtelTestPayloads {
  private OtelTestPayloads() {}

  public static final String TRACE_A_HEX = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  public static final String TRACE_B_HEX = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

  /**
   * Builds a 10-record payload across two traces with predictable content:
   * - Trace A: 5 records; includes one WARN.
   * - Trace B: 5 records; includes two ERRORs and one WARN; two messages contain the word "failed".
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
    sl.addLogRecords(log(baseNs + 1, 9, traceA, "GET /api/catalog/search?q=running+shoes user=u123")); // INFO
    sl.addLogRecords(log(baseNs + 2, 5, traceA, "PricingEngine applied discount code=SPRING10 order=1001")); // DEBUG
    sl.addLogRecords(log(baseNs + 3, 13, traceA, "Inventory low for sku=SHOE-RED-42 remaining=2")); // WARN
    sl.addLogRecords(log(baseNs + 4, 11, traceA, "Payment authorized order=1001 amount=79.99 provider=stripe")); // INFO
    sl.addLogRecords(log(baseNs + 5, 9, traceA, "Order confirmed order=1001 user=u123")); // INFO

    // Trace B (failed checkout)
    sl.addLogRecords(log(baseNs + 6, 9, traceB, "GET /api/catalog/product/SHOE-BLUE-41 user=u999")); // INFO
    sl.addLogRecords(log(baseNs + 7, 17, traceB, "Payment authorization failed order=2002 amount=129.00 provider=stripe code=card_declined")); // ERROR
    sl.addLogRecords(log(baseNs + 8, 13, traceB, "Cart abandoned user=u999 after=2m")); // WARN
    sl.addLogRecords(log(baseNs + 9, 17, traceB, "Order creation failed order=2002 cause=payment_error")); // ERROR
    sl.addLogRecords(log(baseNs + 10, 9, traceB, "Retry scheduled for payment order=2002 in=5m")); // INFO

    rl.addScopeLogs(sl);
    req.addResourceLogs(rl);

    return new Result(req.build().toByteArray(), baseTsMillis);
  }

  private static LogRecord log(long timeUnixNano, int severityNumber, byte[] traceIdBytes, String body) {
    return LogRecord.newBuilder()
        .setTimeUnixNano(timeUnixNano)
        .setSeverityNumber(SeverityNumber.forNumber(severityNumber))
        .setTraceId(ByteString.copyFrom(traceIdBytes))
        .setBody(io.opentelemetry.proto.common.v1.AnyValue.newBuilder().setStringValue(body).build())
        .build();
  }

  public record Result(byte[] body, long startTsMillis) {}
}

