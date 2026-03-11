package org.okapi.rest.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.okapi.rest.session.STREAM_STATE;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChatMessageUpdatesResponse {
  List<ChatMessageResponse> messages;
  STREAM_STATE streamState;
}
