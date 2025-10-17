package org.okapi.traces.it;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValue.Builder;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JsonIngestIntegrationTest {

  @LocalServerPort int port;

  private String baseUrl() {
    return "http://localhost:" + port + "/v1/traces";
  }

  @Test
  void jsonIngest_happyPath_and_missingHeaders() throws Exception {
    RestTemplate rt = new RestTemplate();

    // Build realistic OTLP payload with resource attributes
    long nowMs = System.currentTimeMillis();
    String traceId = "4bf92f3577b34da6a3ce929d0e0e4736"; // 32 hex
    String spanId = "00f067aa0ba902b7"; // 16 hex

    var res =
        Resource.newBuilder()
            .addAttributes(kv("service.name", AnyValue.newBuilder().setStringValue("payments-service").build()))
            .addAttributes(kv("deployment.environment", AnyValue.newBuilder().setStringValue("prod").build()))
            .build();

    Span sp =
        Span.newBuilder()
            .setTraceId(hexToBytes(traceId))
            .setSpanId(hexToBytes(spanId))
            .setName("root")
            .setStartTimeUnixNano(nowMs * 1_000_000L)
            .setEndTimeUnixNano((nowMs + 10) * 1_000_000L)
            .build();

    var scope = ScopeSpans.newBuilder().addSpans(sp).build();
    var rs = ResourceSpans.newBuilder().setResource(res).addScopeSpans(scope).build();
    var req = ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();

    String json = JsonFormat.printer().print(req);

    // Happy path ingest
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    h.add("X-Okapi-Tenant-Id", "it-tenant-json");
    h.add("X-Okapi-App", "it-app");
    ResponseEntity<Map> resp =
        rt.postForEntity(baseUrl(), new HttpEntity<>(json, h), Map.class);
    assertEquals(200, resp.getStatusCode().value());
    assertEquals(1, ((Number) resp.getBody().get("ingested")).intValue());

    // Invalid JSON payload should return ingested:0 (controller swallows parse errors)
    ResponseEntity<Map> inv =
        rt.postForEntity(
            baseUrl(),
            new HttpEntity<>("{not-a-valid-json}", h),
            Map.class);
    assertEquals(200, inv.getStatusCode().value());
    assertEquals(0, ((Number) inv.getBody().get("ingested")).intValue());

    // Missing required X-Okapi-* headers -> 400 (ensure Content-Type matches to avoid 415)
    try {
      HttpHeaders missing = new HttpHeaders();
      missing.setContentType(MediaType.APPLICATION_JSON);
      rt.postForEntity(baseUrl(), new HttpEntity<>(json, missing), Map.class);
      fail("Expected 400 for missing headers");
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      assertEquals(400, e.getStatusCode().value());
    }
  }

  private static KeyValue kv(String k, AnyValue v) {
    Builder b = KeyValue.newBuilder();
    b.setKey(k);
    b.setValue(v);
    return b.build();
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
}
