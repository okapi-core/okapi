/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.jobhandler;

import com.google.inject.Injector;
import java.io.IOException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.agent.dto.PendingJob;
import org.okapi.okapi_agent.TestFixturesModule;
import org.okapi.okapi_agent.TestInjector;
import org.okapi.okapi_agent.messages.ServerMessages;

public class JobLockerTests {

  Injector injector;

  JobLocker jobLocker;
  MockWebServer mockWebServer;
  TestFixturesModule.MockServerPort serverPort;

  @BeforeEach
  void setup() {
    injector = TestInjector.createInjector();
    jobLocker = injector.getInstance(JobLocker.class);
    mockWebServer = injector.getInstance(MockWebServer.class);
    serverPort = injector.getInstance(TestFixturesModule.MockServerPort.class);
  }

  @AfterEach
  void tear() {
    mockWebServer.close();
  }

  @Test
  void testJobLock_Successful() throws IOException {
    mockWebServer.start(serverPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(200).build());
    var couldLock = jobLocker.acquireLock(PendingJob.builder().jobId("test-job-id").build());
    Assertions.assertTrue(couldLock.lockAcquired());
  }

  @Test
  void testJobLock_Failure() throws IOException {
    mockWebServer.start(serverPort.port());
    mockWebServer.enqueue(
        new MockResponse.Builder().code(400).body(ServerMessages.COULD_NOT_TRANSITION).build());
    var couldLock = jobLocker.acquireLock(PendingJob.builder().jobId("test-job-id").build());
    Assertions.assertFalse(couldLock.lockAcquired());
  }

  @Test
  void testJobLock_OtherError() throws IOException {
    mockWebServer.start(serverPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(400).body("messsage").build());
    Assertions.assertThrows(
        RuntimeException.class,
        () -> jobLocker.acquireLock(PendingJob.builder().jobId("test-job-id").build()));
  }
}
