package org.okapi.rest.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SessionMetaResponse {
  String sessionId;
  Long ongoingStreamId;
  long lastRecordedPing;
  SESSION_STATE state;
}
