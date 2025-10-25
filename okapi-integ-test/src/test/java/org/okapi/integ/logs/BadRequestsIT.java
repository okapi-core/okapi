package org.okapi.integ.logs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class BadRequestsIT extends LogsHttpSupport {

  @Test
  void ingest_missingHeaders_returns4xx() throws Exception {
    byte[] dummy = new byte[] {1, 2, 3};
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/v1/logs"))
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(dummy))
            .build();
    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
    assertTrue(resp.statusCode() >= 400 && resp.statusCode() < 500);
  }

  @Test
  void query_malformedJson_returns4xx() throws Exception {
    String tenant = newTenantId();
    String stream = "s-integ";
    byte[] badJson = "{not-json".getBytes(StandardCharsets.UTF_8);
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/logs/query"))
            .header("Content-Type", "application/json")
            .header("X-Okapi-Tenant-Id", tenant)
            .header("X-Okapi-Log-Stream", stream)
            .POST(HttpRequest.BodyPublishers.ofByteArray(badJson))
            .build();
    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
    assertTrue(resp.statusCode() >= 400 && resp.statusCode() < 500);
  }
}

