/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.jobhandler;

import org.okapi.agent.dto.PendingJob;
import org.okapi.agent.dto.QueryResult;

public interface JobHandler {
  QueryResult getResults(PendingJob pendingJob);

  String getSourceId();
}
