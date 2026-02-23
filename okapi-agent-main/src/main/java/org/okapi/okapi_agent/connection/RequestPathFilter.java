/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.connection;

import java.util.List;
import lombok.AllArgsConstructor;
import org.okapi.agent.dto.AgentQueryRecords;

@AllArgsConstructor
public class RequestPathFilter {
  List<String> bannedPathPatterns;
  HttpPathMatcher pathMatcher;

  public boolean shouldProcessQuery(AgentQueryRecords.HttpQuery query) {
    return bannedPathPatterns.stream()
        .noneMatch(pattern -> pathMatcher.matches(pattern, query.path()));
  }
}
