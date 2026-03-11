package org.okapi.oscar.chat;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.okapi.rest.chat.CHAT_RESPONSE_TYPE;
import org.okapi.rest.chat.CHAT_ROLE;

@Entity
@Table(name = "oscar_chat_messages")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChatMessageEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(name = "session_id", nullable = false)
  String sessionId;

  @Column(name = "user_id", nullable = false)
  String userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  CHAT_ROLE role;

  @Column(nullable = false, columnDefinition = "TEXT")
  String contents;

  @Column(nullable = false)
  long eventStreamId;

  @Enumerated(EnumType.STRING)
  @Column(name = "response_type")
  CHAT_RESPONSE_TYPE responseType;

  @Column(name = "ts_millis", nullable = false)
  Long tsMillis;
}
