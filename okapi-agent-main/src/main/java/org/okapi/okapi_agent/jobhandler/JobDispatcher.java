/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.jobhandler;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.okapi.agent.dto.PendingJob;
import org.okapi.agent.dto.QueryResult;

@Slf4j
public class JobDispatcher {

  HandlerRegistry handlerRegistry;
  JobLocker jobLocker;

  @Inject
  public JobDispatcher(HandlerRegistry handlerRegistry, JobLocker jobLocker) {
    this.handlerRegistry = handlerRegistry;
    this.jobLocker = jobLocker;
  }

  public Optional<QueryResult> handleAsync(PendingJob pendingJob) {
    var handlerOptional = handlerRegistry.get(pendingJob.getSourceId());
    if (handlerOptional.isEmpty()) {
      var err = "No handler found for source ID: " + pendingJob.getSourceId();
      return Optional.of(QueryResult.builder().error(err).build());
    } else {
      return handlerOptional.flatMap(
          (handler) -> {
            var lockResult = jobLocker.acquireLock(pendingJob);
            if (lockResult.lockAcquired()) {
              return Optional.ofNullable(handler.getResults(pendingJob));
            }
            return Optional.empty();
          });
    }
  }

  public Optional<QueryResult> handleSync(PendingJob job) {
    var optionalJobHandler = handlerRegistry.get(job.getSourceId());
    return optionalJobHandler.map(handler -> handler.getResults(job));
  }
}
