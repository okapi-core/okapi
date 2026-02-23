/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.it;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.okapi.metrics.OkapiMetricsConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    classes = OkapiMetricsConsumer.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      "spring.profiles.active=integration-test",
      "cas.contact.point=127.0.0.1:9042",
      "cas.contact.datacenter=datacenter1",
      "cas.metrics.keyspace=okapi_telemetry",
      "cas.async.threads=2",
      "promQl.evalThreads=1",
      "aws.region=us-east-1"
    })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PromQlInternalIngestionIT {

  final Gson gson = new Gson();
  @LocalServerPort int port;

  @Value("${cas.contact.point}")
  String contactPoint;

  @Value("${cas.contact.datacenter}")
  String datacenter;

  RestTemplate rest = new RestTemplate();

  // variables that we capture
  Long now;
  Long dataStart;

  @Test
  void internalIngest_thenPromQlQueries_work() {
    String tenant = "it_tenant_internal";
    String metric = "memory_usage";
    Map<String, String> tags = Map.of("job", "web", "instance", "i2");

    now = System.currentTimeMillis();
    long t1 = now - 2_000;
    long t2 = now - 1_000;
    dataStart = t2;

    Map<String, Object> gauge = new HashMap<>();
    gauge.put("ts", new long[] {t1, t2});
    gauge.put("value", new float[] {0.2f, 0.4f});

    Map<String, Object> body = new HashMap<>();
    body.put("tenantId", tenant);
    body.put("metricName", metric);
    body.put("type", "GAUGE");
    body.put("tags", tags);
    body.put("gauge", gauge);

    String base = "http://localhost:" + port;

    // Ingest via internal API
    HttpHeaders hdr = new HttpHeaders();
    hdr.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, hdr);
    ResponseEntity<String> ingResp =
        rest.exchange(URI.create(base + "/api/v1/metrics"), HttpMethod.POST, entity, String.class);
    assertEquals(HttpStatus.OK, ingResp.getStatusCode());

    // Wait until data is queryable
    Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              String json = queryInstant(base, tenant, metric + "{job=\"web\",instance=\"i2\"}");
              JsonObject result = gson.fromJson(json, JsonObject.class);
              assertEquals("success", result.get("status").getAsString());
              JsonObject data = result.getAsJsonObject("data");
              assertEquals("vector", data.get("resultType").getAsString());
              var arr = data.getAsJsonArray("result");
              assertTrue(arr.size() >= 1);
            });

    // 1) Raw vector query
    String vecJson = queryInstant(base, tenant, metric + "{job=\"web\",instance=\"i2\"}");
    JsonObject vec = gson.fromJson(vecJson, JsonObject.class);
    JsonArray vecRes = vec.getAsJsonObject("data").getAsJsonArray("result");
    JsonObject series = vecRes.get(0).getAsJsonObject();
    JsonArray value = series.getAsJsonArray("value");
    String valStr = value.get(1).getAsString();
    assertEquals("0.4", valStr);

    // 2) Aggregation: sum(memory_usage)
    String sumJson = queryInstant(base, tenant, "sum(" + metric + ")");
    JsonObject sum = gson.fromJson(sumJson, JsonObject.class);
    JsonObject sumData = sum.getAsJsonObject("data");
    assertEquals("scalar", sumData.get("resultType").getAsString());
    JsonArray scalar = sumData.getAsJsonArray("result");
    assertEquals("0.4", scalar.get(1).getAsString());
  }

  private String queryInstant(String base, String tenant, String expr) {
    HttpHeaders h = new HttpHeaders();
    h.set("x-okapi-tenant", tenant);
    HttpEntity<Void> e = new HttpEntity<>(h);
    String url =
        base + "/api/v1/query?query=" + UriUtils.encode(expr) + "&time=" + (dataStart / 1000);
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
