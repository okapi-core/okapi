package org.okapi.traces.it;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValue.Builder;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.nio.file.Files;
import java.nio.file.Path;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      "okapi.traces.baseDir=${java.io.tmpdir}/okapi-it-traces",
      "okapi.traces.page.maxEstimatedBytes=1"  // force flush on small payloads
    })
public class FileBackedQueryIntegrationTest {

  @LocalServerPort int port;

  private final RestTemplate rt = new RestTemplate();

  private String ingestUrl() {return "http://localhost:" + port + "/v1/traces";}
  private String queryUrl() {return "http://localhost:" + port + "/span/query";}
  private final ObjectMapper om = new ObjectMapper();

  @BeforeAll
  static void ensureBaseDir() throws Exception {
    Path p = Path.of(System.getProperty("java.io.tmpdir"), "okapi-it-traces");
    Files.createDirectories(p);
  }

  @Test
  void flushed_page_is_queryable_via_file_processor() throws Exception {
    String tenant = "it-tenant-file";
    String app = "it-app";
    long now = System.currentTimeMillis();
    String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
    String spanId = "00f067aa0ba902b7";

    var res =
        Resource.newBuilder()
            .addAttributes(kv("service.name", AnyValue.newBuilder().setStringValue("payments-service").build()))
            .build();

    Span sp =
        Span.newBuilder()
            .setTraceId(hexToBytes(traceId))
            .setSpanId(hexToBytes(spanId))
            .setName("root")
            .setStartTimeUnixNano(now * 1_000_000L)
            .setEndTimeUnixNano((now + 5) * 1_000_000L)
            .build();

    var scope = ScopeSpans.newBuilder().addSpans(sp).build();
    var rs = ResourceSpans.newBuilder().setResource(res).addScopeSpans(scope).build();
    var req = ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();

    // With maxEstimatedBytes=1 the first append triggers immediate flush to disk
    postProtobuf(req.toByteArray(), tenant, app);

    String q =
        "{" +
        "\"startMillis\":" + (now - 1) + "," +
        "\"endMillis\":" + (now + 1000) + "," +
        "\"traceId\":\"" + traceId + "\"" +
        "}";
    HttpHeaders h = headers(tenant, app);
    h.setContentType(MediaType.APPLICATION_JSON);
    var resp = rt.postForEntity(queryUrl(), new HttpEntity<>(q, h), String.class);
    assertEquals(200, resp.getStatusCode().value());
    String body = resp.getBody();
    assertNotNull(body);
    Map<String, Object> root = om.readValue(body, new TypeReference<Map<String, Object>>() {});
    Object spans = root.get("spans");
    String spansJson = om.writeValueAsString(spans);
    List<Map<String, Object>> arr = om.readValue(spansJson, new TypeReference<List<Map<String, Object>>>() {});
    assertTrue(arr.size() >= 1);
    String returnedId = (String) arr.get(0).get("spanId");
    assertEquals(spanId, returnedId);
  }

  private void postProtobuf(byte[] body, String tenant, String app) {
    HttpHeaders h = headers(tenant, app);
    h.setContentType(MediaType.parseMediaType("application/x-protobuf"));
    ResponseEntity<Map> resp = rt.postForEntity(ingestUrl(), new HttpEntity<>(body, h), Map.class);
    assertEquals(200, resp.getStatusCode().value());
  }

  private static HttpHeaders headers(String tenant, String app) {
    HttpHeaders h = new HttpHeaders();
    h.add("X-Okapi-Tenant-Id", tenant);
    h.add("X-Okapi-App", app);
    return h;
  }

  private static KeyValue kv(String k, AnyValue v) {
    Builder b = KeyValue.newBuilder();
    b.setKey(k);
    b.setValue(v);
    return b.build();
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) sb.append(String.format("%02x", b));
    return sb.toString();
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
