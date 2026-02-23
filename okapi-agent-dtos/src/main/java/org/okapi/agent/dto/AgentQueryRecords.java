package org.okapi.agent.dto;

import java.util.Map;

public class AgentQueryRecords {
  public record HttpQuery(
      HTTP_METHOD method, String path, Map<String, String> requestHeaders, String requestBody) {}
}
