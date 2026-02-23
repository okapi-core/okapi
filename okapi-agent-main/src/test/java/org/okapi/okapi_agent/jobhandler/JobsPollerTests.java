package org.okapi.okapi_agent.jobhandler;

import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.okapi.agent.dto.*;
import org.okapi.okapi_agent.client.OkapiGatewayClient;
import org.okapi.okapi_agent.scheduled.JobsPoller;

@ExtendWith(MockitoExtension.class)
public class JobsPollerTests {

  @Mock OkapiGatewayClient mockGatewayClient;
  @Mock JobDispatcher mockDispatcher;
  JobsPoller poller;

  @BeforeEach
  void setup() {
    poller = new JobsPoller(mockGatewayClient, mockDispatcher);
  }

  @Test
  void processEmptyList() {
    var emptyList = createPendingJobsResponse(0);
    poller.processJob(emptyList);
    Mockito.verifyNoInteractions(mockDispatcher);
  }

  @Test
  void processNonEmptyList() {
    var nonEmptyList = createPendingJobsResponse(5);
    poller.processJob(nonEmptyList);
    Mockito.verify(mockDispatcher, Mockito.times(5)).handleAsync(Mockito.any());
  }

  @Test
  void processWithMixedResults() {
    var pendingJobs = createPendingJobsResponse(4);
    Mockito.when(mockDispatcher.handleAsync(Mockito.any()))
        .thenReturn(java.util.Optional.of(successfulResponse()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(errorResponse()))
        .thenReturn(Optional.of(successfulResponse()));
    Mockito.when(mockGatewayClient.nextPendingJob()).thenReturn(pendingJobs);
    poller.nextPendingJob();
    Mockito.verify(mockDispatcher, Mockito.times(4)).handleAsync(Mockito.any());
    Mockito.verify(mockGatewayClient, Mockito.times(1))
        .submitResult(Mockito.eq("job-0"), Mockito.eq(successfulResponse()));
    Mockito.verify(mockGatewayClient, Mockito.times(1))
            .submitResult(Mockito.eq("job-3"), Mockito.eq(successfulResponse()));
    Mockito.verify(mockGatewayClient, Mockito.times(1))
        .submitError(Mockito.eq("job-2"), Mockito.eq(errorResponse()));
  }

  public ListPendingJobsResponse createPendingJobsResponse(int count) {
    var jobs = new ArrayList<PendingJob>();
    for (int i = 0; i < count; i++) {
      jobs.add(
          PendingJob.builder()
              .jobId("job-" + i)
              .jobStatus(JOB_STATUS.PENDING)
              .spec(sampleSpec())
              .sourceId("source-" + i)
              .build());
    }
    return new ListPendingJobsResponse(jobs);
  }

  public QuerySpec sampleSpec() {
    return QuerySpec.builder()
        .serializedQuery(
            "POST /data HTTP/1.1\nHost: example.com\nContent-Type: application/json\r\n\r\n{\"key\":\"value\"}")
        .build();
  }

  public QueryResult successfulResponse() {
    return QueryResult.ofData("Successful content");
  }

  public QueryResult errorResponse() {
    return QueryResult.ofError("Error occurred");
  }
}
