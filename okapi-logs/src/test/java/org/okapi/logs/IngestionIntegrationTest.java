package org.okapi.logs;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import java.time.Instant;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.okapi.logs.controller.OtelLogsController;
import org.okapi.logs.query.LevelFilter;
import org.okapi.logs.query.OnDiskQueryProcessor;
import org.okapi.logs.query.RegexFilter;
import org.okapi.logs.query.TraceFilter;
import org.okapi.protos.logs.LogPayloadProto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = TestApplication.class)
@TestPropertySource(
    properties = {
      "okapi.logs.dataDir=target/test-logs",
      "okapi.logs.maxDocsPerPage=5",
      "okapi.logs.maxPageBytes=65536",
      "okapi.logs.maxPageWindowMs=60000",
      "okapi.logs.fsyncOnPageAppend=true"
    })
class IngestionIntegrationTest {

  @Autowired OtelLogsController controller;
  @Autowired OnDiskQueryProcessor onDisk;

  @Test
  void ingestOtel_andQueryByRegexTraceLevel() throws Exception {
    String tenant = "t-integ";
    String stream = "s-integ";
    byte[] body = buildOtelPayload();
    controller.ingestProtobuf(tenant, stream, body);

    // Wait for persister thread to flush two pages
    Awaitility.await()
        .untilAsserted(
            () -> {
              List<LogPayloadProto> warn =
                  onDisk.getLogs(tenant, stream, 0, Long.MAX_VALUE, new LevelFilter(30));
              assertEquals(2, warn.size());
            });

    List<LogPayloadProto> traceA =
        onDisk.getLogs(
            tenant, stream, 0, Long.MAX_VALUE, new TraceFilter("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
    assertEquals(5, traceA.size());

    List<LogPayloadProto> failed =
        onDisk.getLogs(tenant, stream, 0, Long.MAX_VALUE, new RegexFilter("failed"));
    assertEquals(2, failed.size());
  }

  private byte[] buildOtelPayload() {
    long nowNs = Instant.now().toEpochMilli() * 1_000_000L;
    byte[] traceA = new byte[16];
    java.util.Arrays.fill(traceA, (byte) 0xAA);
    byte[] traceB = new byte[16];
    java.util.Arrays.fill(traceB, (byte) 0xBB);

    // Build 10 logs: 5 per trace. Levels: 2 warn, 2 error, 1 debug; remaining are info
    ExportLogsServiceRequest.Builder req = ExportLogsServiceRequest.newBuilder();
    ResourceLogs.Builder rl = ResourceLogs.newBuilder();
    ScopeLogs.Builder sl = ScopeLogs.newBuilder();

    // helper
    java.util.function.BiConsumer<String, LogRecord> add =
        (t, lr) -> {
          sl.addLogRecords(lr);
        };

    // Trace A (successful checkout)
    sl.addLogRecords(
        log(nowNs + 1, 9, traceA, "GET /api/catalog/search?q=running+shoes user=u123")); // INFO
    sl.addLogRecords(
        log(
            nowNs + 2,
            5,
            traceA,
            "PricingEngine applied discount code=SPRING10 order=1001")); // DEBUG
    sl.addLogRecords(
        log(nowNs + 3, 13, traceA, "Inventory low for sku=SHOE-RED-42 remaining=2")); // WARN
    sl.addLogRecords(
        log(
            nowNs + 4,
            11,
            traceA,
            "Payment authorized order=1001 amount=79.99 provider=stripe")); // INFO
    sl.addLogRecords(log(nowNs + 5, 9, traceA, "Order confirmed order=1001 user=u123")); // INFO

    // Trace B (failed checkout)
    sl.addLogRecords(
        log(nowNs + 6, 9, traceB, "GET /api/catalog/product/SHOE-BLUE-41 user=u999")); // INFO
    sl.addLogRecords(
        log(
            nowNs + 7,
            17,
            traceB,
            "Payment authorization failed order=2002 amount=129.00 provider=stripe code=card_declined")); // ERROR
    sl.addLogRecords(log(nowNs + 8, 13, traceB, "Cart abandoned user=u999 after=2m")); // WARN
    sl.addLogRecords(
        log(
            nowNs + 9,
            17,
            traceB,
            "Order creation failed order=2002 cause=payment_error")); // ERROR
    sl.addLogRecords(
        log(nowNs + 10, 9, traceB, "Retry scheduled for payment order=2002 in=5m")); // INFO

    rl.addScopeLogs(sl);
    req.addResourceLogs(rl);
    return req.build().toByteArray();
  }

  private static LogRecord log(
      long timeUnixNano, int severityNumber, byte[] traceIdBytes, String body) {
    return LogRecord.newBuilder()
        .setTimeUnixNano(timeUnixNano)
        .setSeverityNumber(SeverityNumber.forNumber(severityNumber))
        .setTraceId(com.google.protobuf.ByteString.copyFrom(traceIdBytes))
        .setBody(
            io.opentelemetry.proto.common.v1.AnyValue.newBuilder().setStringValue(body).build())
        .build();
  }
}
