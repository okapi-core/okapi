package org.okapi.integ.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.okapi.rest.logs.FilterNode;
import org.okapi.rest.logs.QueryRequest;
import org.okapi.rest.logs.QueryResponse;

@Slf4j
public class LogsHttpIT {
  private static String baseUrl;
  private static final HttpClient http = HttpClient.newHttpClient();
  private static final Gson gson = new GsonBuilder().create();

  @BeforeAll
  static void resolveBaseUrl() {
    baseUrl = System.getProperty("okapi.integ.baseUrl");
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = System.getenv("OKAPI_BASE_URL");
    }
    if (baseUrl == null) {
      baseUrl = "http://localhost:8080";
      log.info("Using default url: {}", baseUrl);
    }
    assertNotNull(baseUrl, "Provide base URL via -Dokapi.integ.baseUrl or OKAPI_BASE_URL");
  }

  @Test
  void ingestAndQueryOverHttp() throws Exception {
    String tenant = "logs-integ-" + UUID.randomUUID();
    String stream = "s-integ"; // can be reused; tenant separation ensures isolation

    // Build a predictable OTel payload of 10 records across 2 traces
    long baseTsMs = Instant.now().toEpochMilli();
    Payload p = buildPayload(baseTsMs);

    // Ingest via OTLP/HTTP
    HttpRequest ingestReq =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/v1/logs"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/octet-stream")
            .header("X-Okapi-Tenant-Id", tenant)
            .header("X-Okapi-Log-Stream", stream)
            .POST(HttpRequest.BodyPublishers.ofByteArray(p.body))
            .build();
    HttpResponse<String> ingestResp = http.send(ingestReq, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, ingestResp.statusCode(), "ingest failed: " + ingestResp.body());

    long start = p.firstTsMs - 60_000;
    long end = p.firstTsMs + 60_000;

    // Poll queries until we observe expected counts or timeout
    int traceCount =
        pollCount(tenant, stream, start, end, traceFilter("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"), 5);
    assertEquals(5, traceCount);

    int failedCount = pollCount(tenant, stream, start, end, regexFilter("failed"), 2);
    assertEquals(2, failedCount);

    int warnCount = pollCount(tenant, stream, start, end, levelFilter(30), 2);
    assertEquals(2, warnCount);
  }

  private static int pollCount(
      String tenant, String stream, long start, long end, FilterNode filter, int expected)
      throws Exception {
    long deadline = System.currentTimeMillis() + 25_000; // 25s budget
    int last = -1;
    while (System.currentTimeMillis() < deadline) {
      int c = queryCount(tenant, stream, start, end, filter);
      last = c;
      if (c == expected) return c;
      Thread.sleep(250);
    }
    assertTrue(false, "Timed out waiting for expected count=" + expected + ", last=" + last);
    return last;
  }

  private static int queryCount(
      String tenant, String stream, long start, long end, FilterNode filter) throws Exception {
    QueryRequest req = new QueryRequest(start, end, 1000, null, filter);
    byte[] json = gson.toJson(req).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    HttpRequest httpReq =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/logs/query"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .header("X-Okapi-Tenant-Id", tenant)
            .header("X-Okapi-Log-Stream", stream)
            .POST(HttpRequest.BodyPublishers.ofByteArray(json))
            .build();
    HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resp.statusCode(), "query failed: " + resp.body());
    QueryResponse q = gson.fromJson(resp.body(), QueryResponse.class);
    assertNotNull(q);
    return (q.items == null) ? 0 : q.items.size();
  }

  private static FilterNode traceFilter(String traceId) {
    FilterNode n = new FilterNode();
    n.kind = "TRACE";
    n.traceId = traceId;
    return n;
  }

  private static FilterNode regexFilter(String regex) {
    FilterNode n = new FilterNode();
    n.kind = "REGEX";
    n.regex = regex;
    return n;
  }

  private static FilterNode levelFilter(int levelCode) {
    FilterNode n = new FilterNode();
    n.kind = "LEVEL";
    n.levelCode = levelCode;
    return n;
  }

  private static Payload buildPayload(long baseTsMillis) {
    long baseNs = baseTsMillis * 1_000_000L;
    byte[] traceA = new byte[16];
    byte[] traceB = new byte[16];
    Arrays.fill(traceA, (byte) 0xAA);
    Arrays.fill(traceB, (byte) 0xBB);

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
    return new Payload(req.build().toByteArray(), baseTsMillis);
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

  private record Payload(byte[] body, long firstTsMs) {}
}
