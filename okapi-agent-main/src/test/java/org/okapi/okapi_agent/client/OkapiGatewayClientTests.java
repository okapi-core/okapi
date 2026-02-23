package org.okapi.okapi_agent.client;

import com.google.gson.Gson;
import java.io.IOException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.agent.dto.*;
import org.okapi.okapi_agent.TestFixturesModule;
import org.okapi.okapi_agent.TestInjector;

public class OkapiGatewayClientTests {
  OkapiGatewayClient client;
  MockWebServer mockWebServer;
  TestFixturesModule.MockServerPort mockServerPort;

  @BeforeEach
  void setup() {
    var injector = TestInjector.createInjector();
    client = injector.getInstance(OkapiGatewayClient.class);
    mockWebServer = injector.getInstance(MockWebServer.class);
    mockServerPort = injector.getInstance(TestFixturesModule.MockServerPort.class);
  }

  @Test
  public void test_SuccessfulSubmit() throws IOException, InterruptedException {
    System.out.println(mockServerPort.port());
    mockWebServer.start(mockServerPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(200).build());
    var response = QueryResult.builder().data("content").build();
    client.submitResult("test-job-id", response);

    var recordedRequest = mockWebServer.takeRequest();
    Assertions.assertEquals("POST", recordedRequest.getMethod());
    Assertions.assertEquals(
        "/api/v1/pending-jobs/test-job-id/results", recordedRequest.getTarget());
  }

  @Test
  public void test_FailedSubmit() throws IOException {
    mockWebServer.start(mockServerPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(500).build());
    var response = QueryResult.builder().data("content").build();
    Assertions.assertThrows(
        RuntimeException.class, () -> client.submitResult("test-job-id", response));
  }

  @Test
  public void test_UpdateJobStatus() throws IOException, InterruptedException {
    mockWebServer.start(mockServerPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(200).build());
    var result = client.updateJobStatus("test-job-id", org.okapi.agent.dto.JOB_STATUS.COMPLETED);
    Assertions.assertTrue(result.jobWasUpdated());

    var recordedRequest = mockWebServer.takeRequest();
    Assertions.assertEquals("POST", recordedRequest.getMethod());
    Assertions.assertEquals("/api/v1/pending-jobs/update", recordedRequest.getTarget());
  }

  @Test
  public void test_UpdateJobStatus_Failure() throws IOException {
    mockWebServer.start(mockServerPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(500).build());
    Assertions.assertThrows(
        RuntimeException.class,
        () -> client.updateJobStatus("test-job-id", org.okapi.agent.dto.JOB_STATUS.COMPLETED));
  }

  @Test
  public void test_UpdateJobStatus_JobNotUpdated() throws IOException {
    mockWebServer.start(mockServerPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(200).build());
    var result = client.updateJobStatus("test-job-id", org.okapi.agent.dto.JOB_STATUS.COMPLETED);
    Assertions.assertTrue(result.jobWasUpdated());
  }

  @Test
  public void test_UpdateJobStatus_JobWasNotUpdated() throws IOException {
    mockWebServer.start(mockServerPort.port());
    mockWebServer.enqueue(
        new MockResponse.Builder()
            .code(400)
            .body("Could not transition job to state COMPLETED")
            .build());
    var result = client.updateJobStatus("test-job-id", org.okapi.agent.dto.JOB_STATUS.COMPLETED);
    Assertions.assertFalse(result.jobWasUpdated());
  }

  @Test
  public void test_UpdateJobStatus_JobWasNotUpdated_OtherError() throws IOException {
    mockWebServer.start(mockServerPort.port());
    mockWebServer.enqueue(
        new MockResponse.Builder().code(400).body("Some other error message").build());
    Assertions.assertThrows(
        RuntimeException.class,
        () -> client.updateJobStatus("test-job-id", org.okapi.agent.dto.JOB_STATUS.COMPLETED));
  }

  @Test
  public void test_UpdateJobStatus_JobWasNotUpdated_EmptyBody() throws IOException {
    mockWebServer.start(mockServerPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(400).body("").build());
    Assertions.assertThrows(
        RuntimeException.class,
        () -> client.updateJobStatus("test-job-id", org.okapi.agent.dto.JOB_STATUS.COMPLETED));
  }

  @Test
  public void test_NextPendingJob_SingleJob() throws IOException, InterruptedException {
    mockWebServer.start(mockServerPort.port());
    var mockList =
        ListPendingJobsResponse.builder().pendingJob(mockPendingJob("mock-pending")).build();
    var gson = new Gson();
    mockWebServer.enqueue(new MockResponse.Builder().code(200).body(gson.toJson(mockList)).build());
    var pending = client.nextPendingJob();
    Assertions.assertEquals(1, pending.getPendingJobs().size());
    Assertions.assertEquals("mock-pending", pending.getPendingJobs().get(0).getJobId());
    var request = mockWebServer.takeRequest();
    Assertions.assertEquals("POST", request.getMethod());
    Assertions.assertEquals("/api/v1/pending-jobs", request.getTarget());
  }

  public PendingJob mockPendingJob(String jobId) {
    return PendingJob.builder()
        .jobId(jobId)
        .spec(QuerySpec.builder().serializedQuery("HTTP GET /api/v1/resources").build())
        .build();
  }

  @Test
  public void test_SubmitError_NoErrors() throws IOException, InterruptedException {
    mockWebServer.start(mockServerPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(200).build());
    var response = QueryResult.builder().data("content").build();
    client.submitError("test-job-id", response);

    var recordedRequest = mockWebServer.takeRequest();
    Assertions.assertEquals("POST", recordedRequest.getMethod());
    Assertions.assertEquals("/api/v1/pending-jobs/test-job-id/errors", recordedRequest.getTarget());
  }

  @Test
  public void test_SubmitError_WithErrors() throws IOException, InterruptedException {
    mockWebServer.start(mockServerPort.port());
    mockWebServer.enqueue(new MockResponse.Builder().code(400).build());
    var response = QueryResult.builder().processingError("Some processing error").build();
    Assertions.assertThrows(
        RuntimeException.class, () -> client.submitError("test-job-id", response));
  }
}
