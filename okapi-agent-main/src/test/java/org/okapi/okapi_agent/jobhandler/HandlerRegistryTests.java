/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.jobhandler;

import com.google.inject.Injector;
import java.io.IOException;
import java.nio.file.Path;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.agent.dto.PendingJob;
import org.okapi.agent.dto.QuerySpec;
import org.okapi.okapi_agent.HandlerCfgModule;
import org.okapi.okapi_agent.TestFixturesModule;
import org.okapi.okapi_agent.TestInjector;

public class HandlerRegistryTests {
  HandlerRegistry handlerRegistry;
  MockWebServer mockWebServer;
  TestFixturesModule.MockServerPort serverPort;
  Injector injector;

  @BeforeEach
  void setup() {
    injector = TestInjector.createInjector();
    mockWebServer = injector.getInstance(MockWebServer.class);
    serverPort = injector.getInstance(TestFixturesModule.MockServerPort.class);
  }

  @Test
  public void testLoadHandlers() {
    handlerRegistry = injector.getInstance(HandlerRegistry.class);
    var handler = handlerRegistry.get("fake_source");
    Assertions.assertTrue(handler.isPresent());
  }

  @Test
  public void testLoadHandlers_InvalidFile() {
    var sampleFile = "okapi-agent-main/src/test/resources/handlerCfg/invalidHandlerCfg.yaml";
    var testInjector =
        TestInjector.createInjectorWithModules(new HandlerCfgModule(Path.of(sampleFile)));
    Assertions.assertThrows(
        RuntimeException.class, () -> testInjector.getInstance(HandlerRegistry.class));
  }

  public HttpHandler createHandler() {
    handlerRegistry = injector.getInstance(HandlerRegistry.class);
    var optionalJobHandler = handlerRegistry.get("fake_source");
    Assertions.assertTrue(optionalJobHandler.isPresent());
    var handler = (HttpHandler) optionalJobHandler.get();
    handler.setHost("http://localhost:" + serverPort.port());
    return handler;
  }

  @Test
  public void testJobDispatch_RegisteredHandler() throws IOException {
    var handler = createHandler();
    mockWebServer.start(serverPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(200).body("response-body").build());
    var results =
        handler.getResults(
            PendingJob.builder()
                .jobId("test-job-id")
                .spec(
                    QuerySpec.builder()
                        .serializedQuery(
                            "POST /fake_endpoint HTTP/1.1\r\nHost: example.com\r\n\r\npayload-body")
                        .build())
                .sourceId("fake_source")
                .build());
    Assertions.assertNull(results.error());
    Assertions.assertNotNull(results.data());
    var result = results.data();
    Assertions.assertEquals("response-body", result);
  }

  @Test
  public void testJobDispatch_WrongSourceId() throws IOException {
    var handler = createHandler();
    mockWebServer.start(serverPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(200).body("response-body").build());
    var results =
        handler.getResults(
            PendingJob.builder()
                .jobId("test-job-id")
                .spec(
                    QuerySpec.builder()
                        .serializedQuery(
                            "POST /fake_endpoint HTTP/1.1\r\nHost: example.com\r\n\r\npayload-body")
                        .build())
                .sourceId("wrong_source")
                .build());
    Assertions.assertNotNull(results.error());
    Assertions.assertNull(results.data());
    var error = results.error();
    Assertions.assertTrue(
        error.contains("Query dispatched to wrong client. Configured: fake_source"));
  }

  @Test
  public void tstJobDispatch_SourceFailure() throws IOException {
    var handler = createHandler();
    mockWebServer.start(serverPort.port());
    mockWebServer.enqueue(
        new MockResponse.Builder().code(500).body("Internal Server Error").build());
    var results =
        handler.getResults(
            PendingJob.builder()
                .jobId("test-job-id")
                .spec(
                    QuerySpec.builder()
                        .serializedQuery(
                            "POST /fake_endpoint HTTP/1.1\r\nHost: example.com\r\n\r\npayload-body")
                        .build())
                .sourceId("fake_source")
                .build());
    Assertions.assertNotNull(results.error());
    Assertions.assertNull(results.data());
    var error = results.error();
    Assertions.assertEquals(
        "HTTP request failed with status code 500 and error: Internal Server Error", error);
  }
}
