package org.okapi.web.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class ApiResponse {
  String response;
  Long createdAt;
  String model;
  RESPONSE_STATUS status;
}
