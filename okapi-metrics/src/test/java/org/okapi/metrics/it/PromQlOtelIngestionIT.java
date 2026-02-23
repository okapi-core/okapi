/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.it;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Gauge;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.okapi.metrics.OkapiMetricsConsumer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    classes = OkapiMetricsConsumer.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      "spring.profiles.active=integration-test",
      "promQl.evalThreads=1",
      "aws.region=us-east-1"
    })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PromQlOtelIngestionIT {

  final Gson gson = new Gson();
  @LocalServerPort int port;
  RestTemplate rest = new RestTemplate(List.of(new ProtobufHttpMessageConverter()));

  private static List<KeyValue> toKvList(Map<String, String> tags) {
    return tags.entrySet().stream()
        .map(
            e ->
                KeyValue.newBuilder()
                    .setKey(e.getKey())
                    .setValue(AnyValue.newBuilder().setStringValue(e.getValue()).build())
                    .build())
        .toList();
  }

  @Test
  void otelIngest_thenPromQlQueries_work() {
    String tenant = "it_tenant_otel";
    String metric = "cpu_usage";
    Map<String, String> tags = Map.of("job", "api", "instance", "i1");

    // Build a minimal OTLP ExportMetricsServiceRequest with a Gauge metric
    long nowMs = System.currentTimeMillis();
    long t1 = nowMs - 2_000;
    long t2 = nowMs - 1_000;

    NumberDataPoint dp1 =
        NumberDataPoint.newBuilder()
            .setTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(t1))
            .setAsDouble(0.5)
            .addAllAttributes(toKvList(tags))
            .build();
    NumberDataPoint dp2 =
        NumberDataPoint.newBuilder()
            .setTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(t2))
            .setAsDouble(0.7)
            .addAllAttributes(toKvList(tags))
            .build();

    Gauge gauge = Gauge.newBuilder().addAllDataPoints(List.of(dp1, dp2)).build();
    Metric m = Metric.newBuilder().setName(metric).setGauge(gauge).build();
    ScopeMetrics sm = ScopeMetrics.newBuilder().addMetrics(m).build();
    ResourceMetrics rm = ResourceMetrics.newBuilder().addScopeMetrics(sm).build();
    ExportMetricsServiceRequest req =
        ExportMetricsServiceRequest.newBuilder().addResourceMetrics(rm).build();

    String base = "http://localhost:" + port;

    // Ingest via OTLP endpoint
    HttpHeaders hdr = new HttpHeaders();
    hdr.set("X-Okapi-Tenant", tenant);
    hdr.setContentType(new MediaType("application", "x-protobuf"));
    hdr.setAccept(List.of(new MediaType("application", "x-protobuf")));
    HttpEntity<byte[]> entity = new HttpEntity<>(req.toByteArray(), hdr);
    ResponseEntity<byte[]> ingResp =
        rest.exchange(URI.create(base + "/v1/metrics"), HttpMethod.POST, entity, byte[].class);
    assertEquals(HttpStatus.OK, ingResp.getStatusCode());

    // Wait until data is queryable
    Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              String json = queryInstant(base, tenant, metric + "{job=\"api\",instance=\"i1\"}");
              JsonObject result = gson.fromJson(json, JsonObject.class);
              assertEquals("success", result.get("status").getAsString());
              JsonObject data = result.getAsJsonObject("data");
              assertEquals("vector", data.get("resultType").getAsString());
              var arr = data.getAsJsonArray("result");
              assertTrue(arr.size() >= 1);
            });

    // 1) Raw vector query: latest value
    String vecJson = queryInstant(base, tenant, metric + "{job=\"api\",instance=\"i1\"}");
    JsonObject vec = gson.fromJson(vecJson, JsonObject.class);
    JsonArray vecRes = vec.getAsJsonObject("data").getAsJsonArray("result");
    JsonObject series = vecRes.get(0).getAsJsonObject();
    JsonArray value = series.getAsJsonArray("value");
    String valStr = value.get(1).getAsString();
    assertEquals("0.7", valStr);

    // 2) Aggregation: sum(cpu_usage)
    String sumJson = queryInstant(base, tenant, "sum(" + metric + ")");
    JsonObject sum = gson.fromJson(sumJson, JsonObject.class);
    JsonObject sumData = sum.getAsJsonObject("data");
    assertEquals("scalar", sumData.get("resultType").getAsString());
    JsonArray scalar = sumData.getAsJsonArray("result");
    assertEquals("0.7", scalar.get(1).getAsString());
  }

  private String queryInstant(String base, String tenant, String expr) {
    HttpHeaders h = new HttpHeaders();
    h.set("x-okapi-tenant", tenant);
    HttpEntity<Void> e = new HttpEntity<>(h);
    String url =
        base
            + "/api/v1/query?query="
            + UriUtils.encode(expr)
            + "&time="
            + (System.currentTimeMillis() / 1000.);
    ResponseEntity<String> resp =
        new RestTemplate().exchange(URI.create(url), HttpMethod.GET, e, String.class);
    assertEquals(HttpStatus.OK, resp.getStatusCode());
    return resp.getBody();
  }

  static class UriUtils {
    static String encode(String s) {
      return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
  }
}
