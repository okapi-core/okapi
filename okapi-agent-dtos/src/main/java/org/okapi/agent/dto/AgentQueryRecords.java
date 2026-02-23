/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.agent.dto;

import java.util.Map;

public class AgentQueryRecords {
  public record HttpQuery(
      HTTP_METHOD method, String path, Map<String, String> requestHeaders, String requestBody) {}
}
