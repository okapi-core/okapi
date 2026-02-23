/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.dao;

import static org.okapi.data.dto.TablesAndIndexes.PENDING_JOBS_TABLE;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.okapi.agent.dto.QueryResult;
import org.okapi.data.dao.PendingJobsDao;
import org.okapi.data.dao.ResultUploader;
import org.okapi.data.ddb.iterators.FlatteningIterator;
import org.okapi.data.dto.*;
import org.okapi.data.exceptions.IllegalJobStateTransition;
import org.okapi.data.exceptions.JobNotFoundException;
import org.okapi.data.exceptions.TooManyRetriesException;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class PendingJobsDaoDdbImpl implements PendingJobsDao {

  public static final Integer MAX_RETRY_ATTEMPTS = 5;
  private final DynamoDbTable<PendingJobDdb> table;
  ResultUploader resultUploader;
  Gson gson = new Gson();

  @Inject
  public PendingJobsDaoDdbImpl(DynamoDbEnhancedClient enhancedClient, ResultUploader uploader) {
    this.table =
        enhancedClient.table(PENDING_JOBS_TABLE, TableSchema.fromBean(PendingJobDdb.class));
    this.resultUploader = uploader;
  }

  @Override
  public Optional<PendingJobDdb> getPendingJob(String orgId, String jobId) {
    // No direct index for jobId, scan and filter the first match
    var item = table.getItem(Key.builder().partitionValue(orgId).sortValue(jobId).build());
    return Optional.ofNullable(item);
  }

  @Override
  public List<PendingJobDdb> getPendingJobsByTenantAndStatus(String orgId, JOB_STATUS status) {
    var query =
        table.query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(orgId).build()))
                .build());
    var jobs = Lists.newArrayList(new FlatteningIterator<>(query.iterator()));
    return jobs.stream().filter(j -> status.equals(j.getJobStatus())).toList();
  }

  @Override
  public void createPendingJob(PendingJobDdb job) {
    table.putItem(job);
  }

  @Override
  public void updatePendingJob(PendingJobDdb job) throws IllegalJobStateTransition {
    var existing = getPendingJob(job.getOrgId(), job.getJobId());
    if (existing.isEmpty()) {
      throw new JobNotFoundException(
          "Job not found for update: " + job.getOrgId() + "/" + job.getJobId());
    }
    checkStateTransition(job, existing.get());
    var expr =
        Expression.builder()
            .expression("#st = :from")
            .expressionNames(java.util.Map.of("#st", TableAttributes.JOB_STATUS))
            .expressionValues(
                java.util.Map.of(
                    ":from",
                    AttributeValue.builder().s(existing.get().getJobStatus().name()).build()))
            .build();

    table.updateItem(
        UpdateItemEnhancedRequest.builder(PendingJobDdb.class)
            .item(job)
            .conditionExpression(expr)
            .build());
  }

  protected void checkStateTransition(PendingJobDdb job, PendingJobDdb existingJob)
      throws IllegalJobStateTransition {
    var from = existingJob.getJobStatus();
    var to = job.getJobStatus();
    if (!from.equals(to) && !PendingJobsStateMachine.canTransition(from, to)) {
      throw new IllegalJobStateTransition(
          "Invalid state transition from " + from + " to " + to + " for job " + job.getJobId());
    }
  }

  @Override
  public void deletePendingJob(String orgId, String jobId) {
    var maybe = getPendingJob(orgId, jobId);
    maybe.ifPresent(
        job ->
            table.deleteItem(
                Key.builder().partitionValue(job.getOrgId()).sortValue(job.getJobId()).build()));
  }

  @Override
  public void retryJob(String orgId, String jobId)
      throws TooManyRetriesException, IllegalJobStateTransition {
    Optional<PendingJobDdb> optionalPendingJobDto = getPendingJob(orgId, jobId);
    if (optionalPendingJobDto.isEmpty())
      throw new JobNotFoundException("Job not found for retry: " + orgId + "/" + jobId);
    // Update fields using DTO setters
    var item = optionalPendingJobDto.get();
    if (item.getAttemptCount() >= MAX_RETRY_ATTEMPTS) {
      throw new TooManyRetriesException(
          "Max retry attempts reached for job: " + orgId + "/" + jobId);
    }
    item.setJobStatus(JOB_STATUS.PENDING);
    item.setSourceId(null);
    item.setAssignedAt(null);
    item.setAttemptCount(item.getAttemptCount() + 1);
    updatePendingJob(item);
  }

  @Override
  public PendingJobDdb updateJobStatus(String orgId, String jobId, JOB_STATUS status)
      throws IllegalJobStateTransition {
    var item = table.getItem(Key.builder().partitionValue(orgId).sortValue(jobId).build());
    if (item == null) return null;
    item.setJobStatus(status);
    updatePendingJob(item);
    return item;
  }

  @Override
  public List<PendingJobDdb> getJobsBySourceAndStatus(
      String orgId, String source, JOB_STATUS status, int limit) {
    var index = table.index(TablesAndIndexes.PENDING_JOBS_BY_SOURCE_STATUS_GSI);
    var query =
        index.query(
            QueryEnhancedRequest.builder()
                .limit(10)
                .queryConditional(
                    QueryConditional.keyEqualTo(
                        Key.builder()
                            .partitionValue(orgId + "#" + source + "#" + status.name())
                            .build()))
                .build());
    return Lists.newArrayList(new FlatteningIterator<>(query.iterator()));
  }

  @Override
  public PendingJobDdb updateJobResult(String orgId, String jobId, String resultData)
      throws IllegalJobStateTransition {
    var item = getPendingJob(orgId, jobId);
    if (item.isEmpty()) {
      throw new JobNotFoundException("Job not found for result update: " + orgId + "/" + jobId);
    }
    var location = this.resultUploader.uploadResult(orgId, jobId, resultData);
    var job = item.get();
    job.setResultS3(location);
    job.setJobStatus(JOB_STATUS.COMPLETED);
    updatePendingJob(job);
    return job;
  }

  @Override
  public PendingJobDdb updateJobError(String orgId, String jobId, String errorData)
      throws IllegalJobStateTransition {
    var job =
        getPendingJob(orgId, jobId)
            .orElseThrow(
                () ->
                    new JobNotFoundException(
                        "Job not found for result update: " + orgId + "/" + jobId));
    var location = this.resultUploader.uploadResult(orgId, jobId, errorData);
    job.setErrorS3(location);
    job.setJobStatus(JOB_STATUS.FAILED);
    updatePendingJob(job);
    return job;
  }

  @Override
  public Optional<QueryResult> getRawResult(String orgId, String jobId) {
    var job =
        getPendingJob(orgId, jobId)
            .orElseThrow(
                () ->
                    new JobNotFoundException(
                        "Job not found for raw result: " + orgId + "/" + jobId));
    if (job.getJobStatus() != JOB_STATUS.COMPLETED && job.getJobStatus() != JOB_STATUS.FAILED) {
      return Optional.empty();
    }
    var rawResult = this.resultUploader.getRawResult(orgId, jobId);
    var deserialized = gson.fromJson(rawResult, QueryResult.class);
    return Optional.of(deserialized);
  }
}
