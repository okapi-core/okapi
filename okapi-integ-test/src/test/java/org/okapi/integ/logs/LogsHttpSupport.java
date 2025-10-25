package org.okapi.integ.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.okapi.rest.logs.FilterNode;
import org.okapi.rest.logs.QueryRequest;
import org.okapi.rest.logs.QueryResponse;

/** Shared HTTP helpers and payload builders for okapi-logs integration tests. */
public abstract class LogsHttpSupport {
  protected static String baseUrl;
  protected static final HttpClient http = HttpClient.newHttpClient();
  protected static final Gson gson = new GsonBuilder().create();

  @BeforeAll
  static void resolveBaseUrl() {
    baseUrl = System.getProperty("okapi.integ.baseUrl");
    if (baseUrl == null || baseUrl.isBlank()) baseUrl = System.getenv("OKAPI_BASE_URL");
    assertNotNull(baseUrl, "Provide base URL via -Dokapi.integ.baseUrl or OKAPI_BASE_URL");
  }

  protected static String newTenantId() {
    return "logs-integ-" + UUID.randomUUID();
  }

  // ---------- HTTP ----------
  protected static void postOtel(String tenant, String stream, byte[] body) throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/v1/logs"))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/octet-stream")
            .header("X-Okapi-Tenant-Id", tenant)
            .header("X-Okapi-Log-Stream", stream)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resp.statusCode(), "ingest failed: " + resp.body());
  }

  protected static void postBulk(String tenant, String stream, byte[] scopeLogsBytes)
      throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/v1/logs/bulk"))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/octet-stream")
            .header("X-Okapi-Tenant-Id", tenant)
            .header("X-Okapi-Log-Stream", stream)
            .POST(HttpRequest.BodyPublishers.ofByteArray(scopeLogsBytes))
            .build();
    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resp.statusCode(), "bulk ingest failed: " + resp.body());
  }

  protected static QueryResponse query(String tenant, String stream, QueryRequest req)
      throws Exception {
    byte[] json = gson.toJson(req).getBytes(StandardCharsets.UTF_8);
    HttpRequest httpReq =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/logs/query"))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .header("X-Okapi-Tenant-Id", tenant)
            .header("X-Okapi-Log-Stream", stream)
            .POST(HttpRequest.BodyPublishers.ofByteArray(json))
            .build();
    HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resp.statusCode(), "query failed: " + resp.body());
    QueryResponse q = gson.fromJson(resp.body(), QueryResponse.class);
    assertNotNull(q);
    return q;
  }

  protected static int queryCount(String tenant, String stream, long start, long end, FilterNode f)
      throws Exception {
    QueryRequest req = new QueryRequest(start, end, 1000, null, f);
    QueryResponse r = query(tenant, stream, req);
    return r.items == null ? 0 : r.items.size();
  }

  protected static int pollCount(
      String tenant, String stream, long start, long end, FilterNode f, int expected)
      throws Exception {
    long deadline = System.currentTimeMillis() + 25_000;
    int last = -1;
    while (System.currentTimeMillis() < deadline) {
      last = queryCount(tenant, stream, start, end, f);
      if (last == expected) return last;
      Thread.sleep(250);
    }
    org.junit.jupiter.api.Assertions.assertEquals(expected, last);
    return last;
  }

  // ---------- Filters ----------
  protected static FilterNode traceFilter(String traceId) {
    FilterNode n = new FilterNode();
    n.kind = "TRACE";
    n.traceId = traceId;
    return n;
  }

  protected static FilterNode regexFilter(String regex) {
    FilterNode n = new FilterNode();
    n.kind = "REGEX";
    n.regex = regex;
    return n;
  }

  protected static FilterNode levelFilter(int levelCode) {
    FilterNode n = new FilterNode();
    n.kind = "LEVEL";
    n.levelCode = levelCode;
    return n;
  }

  // ---------- Payloads ----------
  protected static final String TRACE_A_HEX = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  protected static final String TRACE_B_HEX = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

  protected static Payload checkoutScenario(long baseTsMillis) {
    long baseNs = baseTsMillis * 1_000_000L;
    byte[] traceA = new byte[16];
    byte[] traceB = new byte[16];
    Arrays.fill(traceA, (byte) 0xAA);
    Arrays.fill(traceB, (byte) 0xBB);

    var req = ExportLogsServiceRequest.newBuilder();
    var rl = ResourceLogs.newBuilder();
    var sl = ScopeLogs.newBuilder();

    // Trace A
    sl.addLogRecords(log(baseNs + 1, 9, traceA, "GET /api/catalog/search?q=running+shoes user=u123"));
    sl.addLogRecords(log(baseNs + 2, 5, traceA, "PricingEngine applied discount code=SPRING10 order=1001"));
    sl.addLogRecords(log(baseNs + 3, 13, traceA, "Inventory low for sku=SHOE-RED-42 remaining=2"));
    sl.addLogRecords(log(baseNs + 4, 11, traceA, "Payment authorized order=1001 amount=79.99 provider=stripe"));
    sl.addLogRecords(log(baseNs + 5, 9, traceA, "Order confirmed order=1001 user=u123"));

    // Trace B
    sl.addLogRecords(log(baseNs + 6, 9, traceB, "GET /api/catalog/product/SHOE-BLUE-41 user=u999"));
    sl.addLogRecords(log(baseNs + 7, 17, traceB, "Payment authorization failed order=2002 amount=129.00 provider=stripe code=card_declined"));
    sl.addLogRecords(log(baseNs + 8, 13, traceB, "Cart abandoned user=u999 after=2m"));
    sl.addLogRecords(log(baseNs + 9, 17, traceB, "Order creation failed order=2002 cause=payment_error"));
    sl.addLogRecords(log(baseNs + 10, 9, traceB, "Retry scheduled for payment order=2002 in=5m"));

    rl.addScopeLogs(sl);
    req.addResourceLogs(rl);
    return new Payload(req.build().toByteArray(), baseTsMillis);
  }

  protected static Payload fixedTsDocs(long tsMillis, List<String> bodies, List<Integer> levels) {
    long baseNs = tsMillis * 1_000_000L;
    byte[] t = new byte[16];
    var req = ExportLogsServiceRequest.newBuilder();
    var rl = ResourceLogs.newBuilder();
    var sl = ScopeLogs.newBuilder();
    for (int i = 0; i < bodies.size(); i++) {
      int lvl = (levels != null && i < levels.size()) ? levels.get(i) : 9;
      sl.addLogRecords(log(baseNs, lvl, t, bodies.get(i)));
    }
    rl.addScopeLogs(sl);
    req.addResourceLogs(rl);
    return new Payload(req.build().toByteArray(), tsMillis);
  }

  protected static byte[] scopeLogsBytes(long tsMillis, List<String> bodies, List<Integer> levels) {
    long baseNs = tsMillis * 1_000_000L;
    var sl = ScopeLogs.newBuilder();
    byte[] t = new byte[0];
    for (int i = 0; i < bodies.size(); i++) {
      int lvl = (levels != null && i < levels.size()) ? levels.get(i) : 9;
      sl.addLogRecords(log(baseNs + i, lvl, t, bodies.get(i)));
    }
    return sl.build().toByteArray();
  }

  private static LogRecord log(long timeUnixNano, int severityNumber, byte[] traceIdBytes, String body) {
    return LogRecord.newBuilder()
        .setTimeUnixNano(timeUnixNano)
        .setSeverityNumber(SeverityNumber.forNumber(severityNumber))
        .setTraceId(com.google.protobuf.ByteString.copyFrom(traceIdBytes))
        .setBody(io.opentelemetry.proto.common.v1.AnyValue.newBuilder().setStringValue(body).build())
        .build();
  }

  protected record Payload(byte[] body, long firstTsMs) {}
}

