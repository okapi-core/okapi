package org.okapi.rest.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChatMessageResponse {
  long id;
  long timestamp;
  String contents;
  String eventStreamId;
  CHAT_RESPONSE_TYPE responseType;
  CHAT_ROLE role;
}
