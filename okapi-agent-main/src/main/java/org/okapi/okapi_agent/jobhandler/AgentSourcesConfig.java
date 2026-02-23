/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.jobhandler;

import java.util.List;
import lombok.Data;

@Data
public class AgentSourcesConfig {
  private List<SourceDefinition> sources;
}
