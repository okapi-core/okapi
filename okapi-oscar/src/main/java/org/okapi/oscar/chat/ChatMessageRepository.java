package org.okapi.oscar.chat;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

  List<ChatMessageEntity> findBySessionIdAndTsMillisBetweenOrderByTsMillisAsc(
      String sessionId, Long fromMillis, Long toMillis);

  List<ChatMessageEntity> findBySessionIdAndEventStreamIdOrderByTsMillisAsc(
      String sessionId, long eventStreamId);
}
