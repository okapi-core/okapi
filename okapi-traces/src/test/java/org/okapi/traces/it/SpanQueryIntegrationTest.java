package org.okapi.traces.it;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValue.Builder;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpanQueryIntegrationTest {

  @LocalServerPort int port;

  private final RestTemplate rt = new RestTemplate();
  private final ObjectMapper om = new ObjectMapper();

  private String ingestUrl() {
    return "http://localhost:" + port + "/v1/traces";
  }

  private String queryUrl() {
    return "http://localhost:" + port + "/span/query";
  }

  private final String tenant = "it-tenant-query";
  private final String app = "it-app";

  // realistic IDs
  private final String traceId1 = "4bf92f3577b34da6a3ce929d0e0e4736";
  private final String root1 = "00f067aa0ba902b7";
  private final String child1 = "b9c7c989f97918e1";
  private final String child2 = "2ef3c9c6e9c02cde";

  private final String traceId2 = "6e0c63257de34c92a3ce929d0e0e4737";
  private final String root2 = "a3c1f9e2b1c04ade";

  private long baseMs;

  @BeforeEach
  void setupData() throws Exception {
    baseMs = System.currentTimeMillis();

    // Trace 1: payments-service, 3-span chain
    var res1 =
        Resource.newBuilder()
            .addAttributes(
                kv(
                    "service.name",
                    AnyValue.newBuilder().setStringValue("payments-service").build()))
            .addAttributes(
                kv("deployment.environment", AnyValue.newBuilder().setStringValue("prod").build()))
            .build();
    var sRoot = span(traceId1, root1, null, baseMs, baseMs + 10, "root");
    var sChild1 = span(traceId1, child1, root1, baseMs + 5, baseMs + 15, "child1");
    var sChild2 = span(traceId1, child2, child1, baseMs + 8, baseMs + 20, "child2");

    var scope1 =
        ScopeSpans.newBuilder().addSpans(sRoot).addSpans(sChild1).addSpans(sChild2).build();
    var rs1 = ResourceSpans.newBuilder().setResource(res1).addScopeSpans(scope1).build();
    var req1 = ExportTraceServiceRequest.newBuilder().addResourceSpans(rs1).build();

    // Trace 2: orders-service, 1 span, overlapping time
    var res2 =
        Resource.newBuilder()
            .addAttributes(
                kv("service.name", AnyValue.newBuilder().setStringValue("orders-service").build()))
            .addAttributes(
                kv("deployment.environment", AnyValue.newBuilder().setStringValue("prod").build()))
            .build();
    var s2 = span(traceId2, root2, null, baseMs + 2, baseMs + 12, "orders-root");
    var scope2 = ScopeSpans.newBuilder().addSpans(s2).build();
    var rs2 = ResourceSpans.newBuilder().setResource(res2).addScopeSpans(scope2).build();
    var req2 = ExportTraceServiceRequest.newBuilder().addResourceSpans(rs2).build();

    // ingest both
    postProtobuf(req1.toByteArray(), tenant, app);
    postProtobuf(req2.toByteArray(), tenant, app);
  }

  @Test
  void query_by_traceId_returns_all_spans_sorted_and_windowed() throws Exception {
    String body =
        "{"
            + "\"startMillis\":"
            + (baseMs - 1)
            + ","
            + "\"endMillis\":"
            + (baseMs + 1000)
            + ","
            + "\"traceId\":\""
            + traceId1
            + "\""
            + "}";

    var resp = postQuery(body, tenant, app);
    assertEquals(200, resp.getStatusCode().value());
    List<Map<String, Object>> arr = parseSpans(resp.getBody());
    assertEquals(3, arr.size());

    // verify sorted by start time and contains expected spanIds
    String id0 = (String) arr.get(0).get("spanId");
    String id1 = (String) arr.get(1).get("spanId");
    String id2 = (String) arr.get(2).get("spanId");
    assertEquals(root1, id0);
    assertEquals(child1, id1);
    assertEquals(child2, id2);

    // narrow window excludes last child
    String narrow =
        "{"
            + "\"startMillis\":"
            + (baseMs - 1)
            + ","
            + "\"endMillis\":"
            + (baseMs + 5)
            + ","
            + "\"traceId\":\""
            + traceId1
            + "\""
            + "}";
    var r2 = postQuery(narrow, tenant, app);
    List<Map<String, Object>> a2 = parseSpans(r2.getBody());
    assertEquals(2, a2.size());
  }

  @Test
  void query_by_spanId_returns_chain_sorted() throws Exception {
    String body =
        "{"
            + "\"startMillis\":"
            + (baseMs - 1)
            + ","
            + "\"endMillis\":"
            + (baseMs + 1000)
            + ","
            + "\"spanId\":\""
            + child2
            + "\""
            + "}";
    var resp = postQuery(body, tenant, app);
    assertEquals(200, resp.getStatusCode().value());
    List<Map<String, Object>> arr = parseSpans(resp.getBody());
    assertEquals(3, arr.size());
    Set<String> actualIds =
        arr.stream().map(m -> (String) m.get("spanId")).collect(Collectors.toSet());
    Set<String> expectedIds = Set.of(root1, child1, child2);
    assertEquals(expectedIds, actualIds);

    // Narrow window trims chain
    String narrow =
        "{"
            + "\"startMillis\":"
            + (baseMs + 7)
            + ","
            + "\"endMillis\":"
            + (baseMs + 100)
            + ","
            + "\"spanId\":\""
            + child2
            + "\""
            + "}";
    var r2 = postQuery(narrow, tenant, app);
    List<Map<String, Object>> a2 = parseSpans(r2.getBody());
    assertTrue(a2.size() >= 1);
  }

  @Test
  void query_by_attribute_value_and_pattern() throws Exception {
    // exact value
    String valueBody =
        "{"
            + "\"startMillis\":"
            + (baseMs - 1)
            + ","
            + "\"endMillis\":"
            + (baseMs + 1000)
            + ","
            + "\"attributeFilter\":{\"name\":\"service.name\",\"value\":\"payments-service\"}"
            + "}";
    var r1 = postQuery(valueBody, tenant, app);
    List<Map<String, Object>> a1 = parseSpans(r1.getBody());
    assertEquals(3, a1.size());

    // regex pattern
    String patBody =
        "{"
            + "\"startMillis\":"
            + (baseMs - 1)
            + ","
            + "\"endMillis\":"
            + (baseMs + 1000)
            + ","
            + "\"attributeFilter\":{\"name\":\"service.name\",\"pattern\":\"pay.*\"}"
            + "}";
    var r2 = postQuery(patBody, tenant, app);
    List<Map<String, Object>> a2 = parseSpans(r2.getBody());
    assertEquals(3, a2.size());
  }

  @Test
  void time_window_boundaries_and_isolation() throws Exception {
    // end == start of root span should include
    String boundary =
        "{"
            + "\"startMillis\":"
            + (baseMs - 100)
            + ","
            + "\"endMillis\":"
            + (baseMs)
            + ","
            + "\"traceId\":\""
            + traceId1
            + "\""
            + "}";
    var r1 = postQuery(boundary, tenant, app);
    List<Map<String, Object>> a1 = parseSpans(r1.getBody());
    assertTrue(a1.size() >= 1);

    // different app -> empty
    var r2 = postQuery(boundary, tenant, "another-app");
    List<Map<String, Object>> a2 = parseSpans(r2.getBody());
    assertEquals(0, a2.size());

    // different tenant -> empty
    var r3 = postQuery(boundary, "another-tenant", app);
    List<Map<String, Object>> a3 = parseSpans(r3.getBody());
    assertEquals(0, a3.size());
  }

  @Test
  void invalid_query_body_and_missing_headers() {
    // invalid body (no traceId/spanId/attributeFilter) -> 400
    try {
      HttpHeaders h = headers(tenant, app);
      h.setContentType(MediaType.APPLICATION_JSON);
      rt.postForEntity(
          queryUrl(), new HttpEntity<>("{\"startMillis\":0,\"endMillis\":1}", h), String.class);
      fail("Expected 400 for invalid query body");
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      assertEquals(400, e.getStatusCode().value());
    }

    // missing headers -> 400
    try {
      HttpHeaders h = new HttpHeaders();
      h.setContentType(MediaType.APPLICATION_JSON);
      rt.postForEntity(
          queryUrl(), new HttpEntity<>("{\"startMillis\":0,\"endMillis\":1}", h), String.class);
      fail("Expected 400 for missing headers");
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      assertEquals(400, e.getStatusCode().value());
    }
  }

  private ResponseEntity<String> postQuery(String jsonBody, String tenant, String app) {
    HttpHeaders h = headers(tenant, app);
    h.setContentType(MediaType.APPLICATION_JSON);
    return rt.postForEntity(URI.create(queryUrl()), new HttpEntity<>(jsonBody, h), String.class);
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

  private static Span span(
      String traceId, String spanId, String parent, long startMs, long endMs, String name) {
    Span.Builder b =
        Span.newBuilder()
            .setTraceId(hexToBytes(traceId))
            .setSpanId(hexToBytes(spanId))
            .setName(name)
            .setStartTimeUnixNano(startMs * 1_000_000L)
            .setEndTimeUnixNano(endMs * 1_000_000L);
    if (parent != null) b.setParentSpanId(hexToBytes(parent));
    return b.build();
  }

  private List<Map<String, Object>> parseSpans(String json) throws Exception {
    Map<String, Object> root = om.readValue(json, new TypeReference<Map<String, Object>>() {});
    Object spans = root.get("spans");
    String spansJson = om.writeValueAsString(spans);
    return om.readValue(spansJson, new TypeReference<List<Map<String, Object>>>() {});
  }

  // DTO returns hex ids directly; helper not needed anymore.

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
