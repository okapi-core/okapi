/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dao;

import java.util.List;
import java.util.Optional;
import org.okapi.agent.dto.QueryResult;
import org.okapi.data.dto.JOB_STATUS;
import org.okapi.data.dto.PendingJobDdb;
import org.okapi.data.exceptions.IllegalJobStateTransition;
import org.okapi.data.exceptions.TooManyRetriesException;

public interface PendingJobsDao {
  Optional<PendingJobDdb> getPendingJob(String orgId, String jobId);

  List<PendingJobDdb> getPendingJobsByTenantAndStatus(String orgId, JOB_STATUS status);

  void createPendingJob(PendingJobDdb job);

  void updatePendingJob(PendingJobDdb job) throws IllegalJobStateTransition;

  void deletePendingJob(String orgId, String jobId);

  void retryJob(String orgId, String jobId)
      throws TooManyRetriesException, IllegalJobStateTransition;

  PendingJobDdb updateJobStatus(String orgId, String jobId, JOB_STATUS status)
      throws IllegalJobStateTransition;

  List<PendingJobDdb> getJobsBySourceAndStatus(
      String orgId, String source, JOB_STATUS status, int limit);

  PendingJobDdb updateJobResult(String orgId, String jobId, String resultData)
      throws IllegalJobStateTransition;

  PendingJobDdb updateJobError(String orgId, String jobId, String errorData)
      throws IllegalJobStateTransition;

  Optional<QueryResult> getRawResult(String orgId, String jobId);
}
