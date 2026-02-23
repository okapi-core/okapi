/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.connection;

import org.okapi.agent.dto.AgentQueryRecords;

public interface HttpQueryFilter {
  boolean shouldProcess(AgentQueryRecords.HttpQuery httpQuery);
}
