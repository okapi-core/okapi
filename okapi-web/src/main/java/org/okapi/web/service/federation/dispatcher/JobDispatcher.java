/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.federation.dispatcher;

import java.util.concurrent.CompletableFuture;
import org.okapi.agent.dto.PendingJob;
import org.okapi.agent.dto.QueryResult;

public interface JobDispatcher {
  CompletableFuture<QueryResult> dispatchJob(String orgId, PendingJob job);
}
