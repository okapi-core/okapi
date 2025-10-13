package org.okapi.traces.it;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
// Do not import Span to avoid name clash with domain model
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.okapi.traces.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      "cas.contact.point=127.0.0.1:9042",
      "cas.contact.datacenter=datacenter1",
      "cas.traces.keyspace=" + JsonIngestIntegrationTest.KS,
      "sampling.fraction=1.0"
    })
public class JsonIngestIntegrationTest extends AbstractIntegrationTest {

  static final String KS = KEYSPACE;

  @LocalServerPort int port;

  @Autowired(required = false)
  RestTemplate restTemplate = new RestTemplate();

  private String baseUrl() {
    return "http://localhost:" + port + "/v1/traces";
  }

  @Test
  void endToEnd_jsonIngest_and_queries() throws Exception {
    String tenant = "it-tenant-json";
    long nowMs = System.currentTimeMillis();
    // Build an OTLP request with one trace and two spans (one ERROR)
    String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
    String spanOk = "00f067aa0ba902b7";
    String spanErr = "00f067aa0ba902b8";
    ExportTraceServiceRequest req = buildTraceRequest(traceId, nowMs, spanOk, spanErr);
    String json = JsonFormat.printer().print(req);

    // Ingest JSON
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    h.add("X-Okapi-Tenant-Id", tenant);
    ResponseEntity<Map> ing =
        restTemplate.postForEntity(baseUrl(), new HttpEntity<>(json, h), Map.class);
    assertEquals(200, ing.getStatusCode().value());
    assertEquals(2, ((Number) ing.getBody().get("ingested")).intValue());

    // Query spans by trace
    ResponseEntity<OkapiSpan[]> spansRes =
        restTemplate.exchange(
            URI.create(baseUrl() + "/" + traceId + "/spans"),
            HttpMethod.GET,
            new HttpEntity<>(headers(tenant)),
            OkapiSpan[].class);
    assertEquals(200, spansRes.getStatusCode().value());
    OkapiSpan[] okapiSpans = spansRes.getBody();
    assertNotNull(okapiSpans);
    assertEquals(2, okapiSpans.length);

    // Query error spans in window
    long start = nowMs - 5_000;
    long end = nowMs + 60_000;
    ResponseEntity<OkapiSpan[]> errRes =
        restTemplate.exchange(
            URI.create(baseUrl() + "/spans/errors?startMillis=" + start + "&endMillis=" + end),
            HttpMethod.GET,
            new HttpEntity<>(headers(tenant)),
            OkapiSpan[].class);
    assertEquals(200, errRes.getStatusCode().value());
    assertTrue(errRes.getBody().length >= 1);

    // Traces by window with errorCount
    ResponseEntity<Map> tr =
        restTemplate.exchange(
            URI.create(baseUrl() + "/traces/by-window?startMillis=" + start + "&endMillis=" + end),
            HttpMethod.GET,
            new HttpEntity<>(headers(tenant)),
            Map.class);
    assertEquals(200, tr.getStatusCode().value());
    List<String> traces = (List<String>) tr.getBody().get("traces");
    Number errorCount = (Number) tr.getBody().get("errorCount");
    assertTrue(traces.contains(traceId));
    assertTrue(errorCount.intValue() >= 1);

    // Histogram by minute should contain at least one bucket
    ResponseEntity<Map> hist =
        restTemplate.exchange(
            URI.create(baseUrl() + "/spans/histogram?startMillis=" + start + "&endMillis=" + end),
            HttpMethod.GET,
            new HttpEntity<>(headers(tenant)),
            Map.class);
    assertEquals(200, hist.getStatusCode().value());
    assertFalse(hist.getBody().isEmpty());

    // By duration
    ResponseEntity<OkapiSpan[]> byDur =
        restTemplate.exchange(
            URI.create(
                baseUrl()
                    + "/spans/by-duration?startMillis="
                    + start
                    + "&endMillis="
                    + end
                    + "&limit=10"),
            HttpMethod.GET,
            new HttpEntity<>(headers(tenant)),
            OkapiSpan[].class);
    assertEquals(200, byDur.getStatusCode().value());
    assertTrue(byDur.getBody().length >= 2);
    assertTrue(byDur.getBody()[0].getDurationMillis() >= byDur.getBody()[1].getDurationMillis());

    // Edge case: non-existent trace
    ResponseEntity<OkapiSpan[]> empty =
        restTemplate.exchange(
            URI.create(baseUrl() + "/nonexistent-trace/spans"),
            HttpMethod.GET,
            new HttpEntity<>(headers(tenant)),
            OkapiSpan[].class);
    assertEquals(200, empty.getStatusCode().value());
    assertEquals(0, empty.getBody().length);
  }

  private static ExportTraceServiceRequest buildTraceRequest(
      String traceIdHex, long baseMillis, String okSpanIdHex, String errSpanIdHex) {
    long startNanos = baseMillis * 1_000_000L;
    long endOkNanos = (baseMillis + 50) * 1_000_000L;
    long endErrNanos = (baseMillis + 150) * 1_000_000L;

    io.opentelemetry.proto.trace.v1.Span ok =
        io.opentelemetry.proto.trace.v1.Span.newBuilder()
            .setTraceId(hexToBytes(traceIdHex))
            .setSpanId(hexToBytes(okSpanIdHex))
            .setName("ok-span")
            .setKind(Span.SpanKind.SPAN_KIND_SERVER)
            .setStartTimeUnixNano(startNanos)
            .setEndTimeUnixNano(endOkNanos)
            .setStatus(Status.newBuilder().setCode(Status.StatusCode.STATUS_CODE_OK).build())
            .build();
    io.opentelemetry.proto.trace.v1.Span err =
        io.opentelemetry.proto.trace.v1.Span.newBuilder()
            .setTraceId(hexToBytes(traceIdHex))
            .setSpanId(hexToBytes(errSpanIdHex))
            .setName("err-span")
            .setKind(Span.SpanKind.SPAN_KIND_CLIENT)
            .setStartTimeUnixNano(startNanos)
            .setEndTimeUnixNano(endErrNanos)
            .setStatus(Status.newBuilder().setCode(Status.StatusCode.STATUS_CODE_ERROR).build())
            .build();

    var scope = io.opentelemetry.proto.trace.v1.ScopeSpans.newBuilder().addSpans(ok).addSpans(err);
    var rs = io.opentelemetry.proto.trace.v1.ResourceSpans.newBuilder().addScopeSpans(scope);
    return ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
  }

  private static com.google.protobuf.ByteString hexToBytes(String hex) {
    int len = hex.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte)
              ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
    }
    return com.google.protobuf.ByteString.copyFrom(data);
  }

  private static HttpHeaders headers(String tenant) {
    HttpHeaders h = new HttpHeaders();
    h.add("X-Okapi-Tenant-Id", tenant);
    return h;
  }
}
