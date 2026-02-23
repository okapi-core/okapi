package org.okapi.web.ai.tools;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ToolCallResult<T> {
  T result;
  boolean success;
  String errorMessage;
}
