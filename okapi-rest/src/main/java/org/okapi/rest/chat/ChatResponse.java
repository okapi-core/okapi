package org.okapi.rest.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChatResponse {
  long timestamp;
  String contents;
  CHAT_RESPONSE_TYPE responseType;
  CHAT_ROLE role;
}
