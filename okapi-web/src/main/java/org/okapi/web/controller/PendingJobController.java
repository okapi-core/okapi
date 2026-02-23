/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.controller;

import org.okapi.agent.dto.PendingJob;
import org.okapi.agent.dto.QueryResult;
import org.okapi.data.exceptions.IllegalJobStateTransition;
import org.okapi.web.dtos.pendingjob.GetPendingJobsRequest;
import org.okapi.web.dtos.pendingjob.ListPendingJobsResponse;
import org.okapi.web.dtos.pendingjob.UpdatePendingJobRequest;
import org.okapi.web.headers.RequestHeaders;
import org.okapi.web.service.pendingjob.PendingJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pending-jobs")
public class PendingJobController {

  @Autowired PendingJobService pendingJobService;

  @PostMapping("")
  public ListPendingJobsResponse getPendingJobs(
      @RequestHeader(RequestHeaders.AUTHORIZATION) String authorizationHeader,
      @Validated @RequestBody GetPendingJobsRequest request) {
    return pendingJobService.getPendingJobs(authorizationHeader, request);
  }

  @PostMapping("/update")
  public PendingJob updatePendingJob(
      @RequestHeader(RequestHeaders.AUTHORIZATION) String authorizationHeader,
      @Validated @RequestBody UpdatePendingJobRequest request) {
    return pendingJobService.updatePendingJob(authorizationHeader, request);
  }

  @PostMapping("/{jobId}/results")
  public PendingJob submitResult(
      @RequestHeader(RequestHeaders.AUTHORIZATION) String authorizationHeader,
      @PathVariable("jobId") String jobId,
      @Validated @RequestBody QueryResult result)
      throws IllegalJobStateTransition {
    return pendingJobService.submitJobResult(authorizationHeader, jobId, result);
  }

  @PostMapping("/{jobId}/errors")
  public PendingJob submitPendingJobError(
      @RequestHeader(RequestHeaders.AUTHORIZATION) String authorizationHeader,
      @PathVariable("jobId") String jobId,
      @Validated @RequestBody QueryResult result)
      throws IllegalJobStateTransition {
    return pendingJobService.submitPendingJobError(authorizationHeader, jobId, result);
  }
}
