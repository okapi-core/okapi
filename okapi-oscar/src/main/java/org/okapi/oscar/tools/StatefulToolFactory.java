package org.okapi.oscar.tools;

import lombok.AllArgsConstructor;
import org.okapi.oscar.chat.ChatMessageRepository;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class StatefulToolFactory {

  private final ChatMessageRepository repository;

  public StatefulTools getTools(String sessionId) {
    return StatefulTools.forSession(sessionId, repository);
  }
}
