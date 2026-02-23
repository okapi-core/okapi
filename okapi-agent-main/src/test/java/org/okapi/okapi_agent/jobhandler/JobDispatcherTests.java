/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.jobhandler;

import com.google.inject.Injector;
import java.io.IOException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.agent.dto.PendingJob;
import org.okapi.agent.dto.QuerySpec;
import org.okapi.okapi_agent.TestFixturesModule;
import org.okapi.okapi_agent.TestInjector;
import org.okapi.okapi_agent.messages.ServerMessages;

public class JobDispatcherTests {
  Injector injector;
  JobDispatcher jobDispatcher;
  MockWebServer mockWebServer;
  TestFixturesModule.MockServerPort serverPort;

  @BeforeEach
  void setup() {
    injector = TestInjector.createInjector();
    jobDispatcher = injector.getInstance(JobDispatcher.class);
    serverPort = injector.getInstance(TestFixturesModule.MockServerPort.class);
    mockWebServer = injector.getInstance(MockWebServer.class);
    // setup http handler correctly
    var handlerRegistry = injector.getInstance(HandlerRegistry.class);
    var optionalJobHandler = handlerRegistry.get("fake_source");
    Assertions.assertTrue(optionalJobHandler.isPresent());
    var handler = (HttpHandler) optionalJobHandler.get();
    handler.setHost("http://localhost:" + serverPort.port());
  }

  @Test
  void lockFailure() throws IOException {
    mockWebServer.start(serverPort.port());
    mockWebServer.enqueue(
        new MockResponse.Builder().code(400).body(ServerMessages.COULD_NOT_TRANSITION).build());
    var result = jobDispatcher.handleAsync(matchingJob());
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void upstreamFailure() throws IOException {
    mockWebServer.start(serverPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(200).build());
    mockWebServer.enqueue(
        new MockResponse.Builder().code(500).body("Internal Server Error").build());
    var result = jobDispatcher.handleAsync(matchingJob());
    Assertions.assertTrue(result.isPresent());
    Assertions.assertNotNull(result.get().error());
    Assertions.assertEquals(
        "HTTP request failed with status code 500 and error: Internal Server Error",
        result.get().error());
  }

  @Test
  void upstreamSuccess() throws IOException {
    mockWebServer.start(serverPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(200).build());
    mockWebServer.enqueue(
        new MockResponse.Builder().code(200).body("{\"key\": \"value\"}").build());
    var optionalResponse = jobDispatcher.handleAsync(matchingJob());
    Assertions.assertTrue(optionalResponse.isPresent());
    Assertions.assertNull(optionalResponse.get().error());
    var result = optionalResponse.get().data();
    Assertions.assertNotNull(result);
    Assertions.assertEquals("{\"key\": \"value\"}", result);
  }

  @Test
  void missingHandler() throws IOException {
    mockWebServer.start(serverPort.port());
    var optionalResponse = jobDispatcher.handleAsync(missingJob());
    Assertions.assertTrue(optionalResponse.isPresent());
    Assertions.assertNotNull(optionalResponse.get().error());
    var result = optionalResponse.get().error();
    Assertions.assertEquals("No handler found for source ID: fake_source_no_match", result);
  }

  public PendingJob matchingJob() {
    return PendingJob.builder()
        .jobId("test-job-id")
        .sourceId("fake_source")
        .spec(
            QuerySpec.builder()
                .serializedQuery(
                    "POST /fake_source HTTP/1.1\nHost: example.com\nContent-Type: application/json\r\n\r\n{\"key\":\"value\"}")
                .build())
        .build();
  }

  public PendingJob missingJob() {
    return PendingJob.builder()
        .jobId("test-job-id")
        .sourceId("fake_source_no_match")
        .spec(
            QuerySpec.builder()
                .serializedQuery(
                    "POST /fake_source HTTP/1.1\nHost: example.com\nContent-Type: application/json\r\n\r\n{\"key\":\"value\"}")
                .build())
        .build();
  }
}
