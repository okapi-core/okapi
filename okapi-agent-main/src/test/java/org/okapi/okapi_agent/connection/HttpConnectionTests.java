/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.connection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.agent.dto.AgentQueryRecords;
import org.okapi.agent.dto.HTTP_METHOD;

public class HttpConnectionTests {
  MockWebServer mockWebServer;
  OkHttpClient httpClient;

  @BeforeEach
  void setup() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    httpClient = new OkHttpClient();
  }

  @Test
  public void of_parsesHeadersAndFiltersNonStrings() {
    var headerMap = new HashMap<>();
    headerMap.put("X-Trace", "trace-id");
    headerMap.put("Ignored", 1234); // non-strings should not be carried over
    Map<String, Object> cfg = Map.of("headers", headerMap);

    var connection = HttpConnection.of(httpClient, cfg);

    Assertions.assertEquals(1, connection.connectionDetails.headers().size());
    Assertions.assertEquals("trace-id", connection.connectionDetails.headers().get("X-Trace"));
  }

  @Test
  public void of_throwsOnNonMapHeadersConfig() {
    Map<String, Object> cfg = Map.of("headers", "not-a-map");
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> HttpConnection.of(httpClient, cfg));
  }

  @Test
  public void sendRequest_translatesGetAndMergesHeaders() throws Exception {
    mockWebServer.enqueue(new MockResponse.Builder().code(200).body("ok-body").build());
    var connection =
        new HttpConnection(
            new HttpConnectionDetails(Map.of("X-Connection", "conn-header")), httpClient);
    var query =
        new AgentQueryRecords.HttpQuery(
            HTTP_METHOD.GET, "/resource", Map.of("X-Request", "req-header"), null);

    var response = connection.sendRequest(baseHost(), query);
    var recorded = mockWebServer.takeRequest();

    Assertions.assertEquals("GET", recorded.getMethod());
    Assertions.assertEquals("/resource", recorded.getTarget());
    Assertions.assertTrue(recorded.getHeaders().names().contains("X-Connection"));
    Assertions.assertTrue(recorded.getHeaders().names().contains("X-Request"));
    Assertions.assertEquals("ok-body", response.data());
    Assertions.assertNull(response.error());
  }

  @Test
  public void sendRequest_translatesPostWithBody() throws Exception {
    mockWebServer.enqueue(new MockResponse.Builder().code(200).body("post-body").build());
    var connection = new HttpConnection(new HttpConnectionDetails(Map.of()), httpClient);
    var query =
        new AgentQueryRecords.HttpQuery(HTTP_METHOD.POST, "/submit", Map.of(), "payload-content");

    var response = connection.sendRequest(baseHost(), query);
    var recorded = mockWebServer.takeRequest();

    Assertions.assertEquals("POST", recorded.getMethod());
    Assertions.assertEquals("payload-content", recorded.getBody().string(StandardCharsets.UTF_8));
    Assertions.assertEquals("post-body", response.data());
    Assertions.assertNull(response.error());
  }

  @Test
  public void sendRequest_returnsErrorResponseForHttpFailures() {
    mockWebServer.enqueue(new MockResponse.Builder().code(500).body("boom").build());
    var connection = new HttpConnection(new HttpConnectionDetails(Map.of()), httpClient);
    var query = new AgentQueryRecords.HttpQuery(HTTP_METHOD.GET, "/explode", Map.of(), null);

    var response = connection.sendRequest(baseHost(), query);

    Assertions.assertNotNull(response.error());
    Assertions.assertNull(response.data());
    Assertions.assertTrue(
        response.error().contains("500"), "Error response should mention non-2xx HTTP status");
  }

  @Test
  public void sendRequest_surfacesTransportExceptionsAsErrors() throws IOException {
    var unreachableHost = baseHost();
    mockWebServer.close();
    var connection = new HttpConnection(new HttpConnectionDetails(Map.of()), httpClient);
    var query = new AgentQueryRecords.HttpQuery(HTTP_METHOD.GET, "/unreachable", Map.of(), null);

    var response = connection.sendRequest(unreachableHost, query);

    Assertions.assertNotNull(response.error());
    Assertions.assertTrue(
        response.error().toLowerCase().contains("failed"),
        "Exceptions during HTTP calls should be wrapped in QueryProcessingError");
  }

  @Test
  public void sendRequest_rejectsUnsupportedHttpMethod() {
    var connection = new HttpConnection(new HttpConnectionDetails(Map.of()), httpClient);
    var query = new AgentQueryRecords.HttpQuery(HTTP_METHOD.PATCH, "/patch-me", Map.of(), "data");

    Assertions.assertThrows(
        IllegalArgumentException.class, () -> connection.sendRequest(baseHost(), query));
  }

  @Test
  public void sendRequest_rejectsUnimplementedDeleteWithClearMessage() {
    var connection = new HttpConnection(new HttpConnectionDetails(Map.of()), httpClient);
    var query = new AgentQueryRecords.HttpQuery(HTTP_METHOD.DELETE, "/delete-me", Map.of(), null);

    var ex =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> connection.sendRequest(baseHostWithTrailingSlash(), query));
    Assertions.assertTrue(
        ex.getMessage().toLowerCase().contains("delete"),
        "DELETE branch should not reuse PUT error message");
  }

  @Test
  public void sendRequest_preservesPathWhenHostHasTrailingSlash() throws Exception {
    mockWebServer.enqueue(new MockResponse.Builder().code(200).body("ok").build());
    var connection = new HttpConnection(new HttpConnectionDetails(Map.of()), httpClient);
    var query = new AgentQueryRecords.HttpQuery(HTTP_METHOD.GET, "/path", Map.of(), null);

    connection.sendRequest(baseHostWithTrailingSlash(), query);
    var recorded = mockWebServer.takeRequest();

    Assertions.assertEquals(
        "/path", recorded.getTarget(), "Path should not contain duplicate slashes");
  }

  private String baseHost() {
    return mockWebServer.url("").toString().replaceAll("/$", "");
  }

  private String baseHostWithTrailingSlash() {
    return mockWebServer.url("/").toString();
  }
}
