/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.jobhandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.okapi.agent.dto.JOB_STATUS;
import org.okapi.agent.dto.PendingJob;
import org.okapi.okapi_agent.client.OkapiGatewayClient;

@Singleton
public class JobLocker {
  OkapiGatewayClient gatewayClient;

  @Inject
  public JobLocker(OkapiGatewayClient gatewayClient) {
    this.gatewayClient = gatewayClient;
  }

  public record JobLockResult(boolean lockAcquired) {}

  public JobLockResult acquireLock(PendingJob job) {
    var updateResult = gatewayClient.updateJobStatus(job.getJobId(), JOB_STATUS.IN_PROGRESS);
    return new JobLockResult(updateResult.jobWasUpdated());
  }
}
