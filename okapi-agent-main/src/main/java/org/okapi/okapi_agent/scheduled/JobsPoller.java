/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.scheduled;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.okapi.agent.dto.ListPendingJobsResponse;
import org.okapi.agent.dto.PendingJob;
import org.okapi.agent.dto.QueryResult;
import org.okapi.okapi_agent.client.OkapiGatewayClient;
import org.okapi.okapi_agent.jobhandler.JobDispatcher;

@Slf4j
public class JobsPoller {
  OkapiGatewayClient gatewayClient;
  JobDispatcher dispatcher;

  @Inject
  public JobsPoller(OkapiGatewayClient gatewayClient, JobDispatcher dispatcher) {
    this.gatewayClient = gatewayClient;
    this.dispatcher = dispatcher;
  }

  public void nextPendingJob() {
    var pendingJob = gatewayClient.nextPendingJob();
    processJob(pendingJob);
  }

  public void processJob(ListPendingJobsResponse pendingJobs) {
    for (var job : pendingJobs.getPendingJobs()) {
      var result = dispatcher.handleAsync(job);
      result.ifPresent((r) -> this.submitResult(job.getJobId(), r));
    }
  }

  public Optional<QueryResult> processJobSync(PendingJob pendingJob) {
    return dispatcher.handleSync(pendingJob);
  }

  public void submitResult(String jobId, QueryResult result) {
    if (result.error() != null) {
      gatewayClient.submitError(jobId, result);
    } else if (result.data() != null) {
      gatewayClient.submitResult(jobId, result);
    }
  }
}
