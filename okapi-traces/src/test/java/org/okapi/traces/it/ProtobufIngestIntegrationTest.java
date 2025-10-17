package org.okapi.traces.it;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
// Avoid importing Span to prevent conflict with domain model
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.okapi.traces.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProtobufIngestIntegrationTest {

  @LocalServerPort int port;

  @Autowired(required = false)
  RestTemplate restTemplate = new RestTemplate();

  private String baseUrl() {
    return "http://localhost:" + port + "/v1/traces";
  }

  @Test
  void endToEnd_protobufIngest_and_edgeCases() {
    String tenant = "it-tenant-proto";
    long nowMs = System.currentTimeMillis();
    String traceId = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    String spanOk = "bbbbbbbbbbbbbbbb";
    ExportTraceServiceRequest req = buildTraceRequest(traceId, nowMs, spanOk);
    byte[] body = req.toByteArray();

    // Ingest protobuf
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.parseMediaType("application/x-protobuf"));
    h.add("X-Okapi-Tenant-Id", tenant);
    h.add("X-Okapi-App", "it-app");
    ResponseEntity<Map> ing =
        restTemplate.postForEntity(baseUrl(), new HttpEntity<>(body, h), Map.class);
    assertEquals(200, ing.getStatusCode().value());
    assertEquals(1, ((Number) ing.getBody().get("ingested")).intValue());

    // Unknown spanId returns 404 (RestTemplate throws on 4xx)
    try {
      restTemplate.exchange(
          URI.create(baseUrl() + "/span/ffffffffffffffff"),
          HttpMethod.GET,
          new HttpEntity<>(headers(tenant)),
          String.class);
      fail("Expected 404 Not Found");
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      assertEquals(404, e.getStatusCode().value());
    }
  }

  private static ExportTraceServiceRequest buildTraceRequest(
      String traceIdHex, long baseMillis, String spanIdHex) {
    long startNanos = baseMillis * 1_000_000L;
    long endNanos = (baseMillis + 100) * 1_000_000L;

    io.opentelemetry.proto.trace.v1.Span ok =
        io.opentelemetry.proto.trace.v1.Span.newBuilder()
            .setTraceId(hexToBytes(traceIdHex))
            .setSpanId(hexToBytes(spanIdHex))
            .setName("ok-span")
            .setKind(Span.SpanKind.SPAN_KIND_INTERNAL)
            .setStartTimeUnixNano(startNanos)
            .setEndTimeUnixNano(endNanos)
            .setStatus(Status.newBuilder().setCode(Status.StatusCode.STATUS_CODE_OK).build())
            .build();

    var scope = ScopeSpans.newBuilder().addSpans(ok);
    var rs = ResourceSpans.newBuilder().addScopeSpans(scope);
    return ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
  }

  private static ByteString hexToBytes(String hex) {
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
    h.add("X-Okapi-App", "it-app");
    return h;
  }
}
