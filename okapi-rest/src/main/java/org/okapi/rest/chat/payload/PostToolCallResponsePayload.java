package org.okapi.rest.chat.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.rest.annotations.TsResponseType;

@AllArgsConstructor
@Getter
@TsResponseType
public class PostToolCallResponsePayload {
  private final String toolName;
  private final String responseJson;
  private final String summary;
}
