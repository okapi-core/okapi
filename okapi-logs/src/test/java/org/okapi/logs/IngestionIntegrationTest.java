package org.okapi.logs;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.okapi.logs.controller.OtelLogsController;
import org.okapi.logs.query.LevelFilter;
import org.okapi.logs.query.OnDiskQueryProcessor;
import org.okapi.logs.query.RegexFilter;
import org.okapi.logs.query.TraceFilter;
import org.okapi.logs.spring.AwsConfiguration;
import org.okapi.protos.logs.LogPayloadProto;
import org.okapi.swim.membership.MembershipEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {TestApplication.class, AwsConfiguration.class})
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "okapi.logs.maxDocsPerPage=5",
      "okapi.logs.maxPageBytes=65536",
      "okapi.logs.maxPageWindowMs=60000",
      "okapi.logs.fsyncOnPageAppend=true"
    })
class IngestionIntegrationTest {

  private static Path testDataDir;

  @DynamicPropertySource
  static void dynamicProps(DynamicPropertyRegistry registry) throws Exception {
    testDataDir = Files.createTempDirectory("okapi-logs-integ-");
    registry.add("okapi.logs.dataDir", () -> testDataDir.toString());
  }

  @Autowired OtelLogsController controller;
  @Autowired OnDiskQueryProcessor onDisk;
  @MockitoBean
  MembershipEventPublisher membershipEventPublisher;

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
                  onDisk.getLogs(
                      tenant, stream, 0, Long.MAX_VALUE, new LevelFilter(30),
                      org.okapi.logs.query.QueryConfig.defaultConfig());
              assertEquals(2, warn.size());
            });

    List<LogPayloadProto> traceA =
        onDisk.getLogs(
            tenant, stream, 0, Long.MAX_VALUE,
            new TraceFilter("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
            org.okapi.logs.query.QueryConfig.defaultConfig());
    assertEquals(5, traceA.size());

    List<LogPayloadProto> failed =
        onDisk.getLogs(
            tenant, stream, 0, Long.MAX_VALUE, new RegexFilter("failed"),
            org.okapi.logs.query.QueryConfig.defaultConfig());
    assertEquals(2, failed.size());
  }

  private byte[] buildOtelPayload() {
    long nowNs = Instant.now().toEpochMilli() * 1_000_000L;
    byte[] traceA = new byte[16];
    Arrays.fill(traceA, (byte) 0xAA);
    byte[] traceB = new byte[16];
    Arrays.fill(traceB, (byte) 0xBB);

    // Build 10 logs: 5 per trace. Levels: 2 warn, 2 error, 1 debug; remaining are info
    var req = ExportLogsServiceRequest.newBuilder();
    var rl = ResourceLogs.newBuilder();
    var sl = ScopeLogs.newBuilder();

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
