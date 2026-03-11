package org.okapi.oscar.service;

import org.junit.jupiter.api.Test;
import org.okapi.oscar.chat.ChatMessageEntity;
import org.okapi.rest.chat.CHAT_RESPONSE_TYPE;
import org.okapi.rest.chat.CHAT_ROLE;
import org.okapi.rest.chat.ChatMessageResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DtoMappersTest {

  @Test
  void mapsAllFieldsFromEntity() {
    var entity =
        ChatMessageEntity.builder()
            .sessionId("session-1")
            .userId("user-1")
            .role(CHAT_ROLE.USER)
            .contents("hello")
            .eventStreamId("stream-1")
            .responseType(CHAT_RESPONSE_TYPE.RESPONSE)
            .tsMillis(12345L)
            .build();

    ChatMessageResponse response = DtoMappers.mapChatEntity(entity);

    assertEquals("hello", response.getContents());
    assertEquals("stream-1", response.getEventStreamId());
    assertEquals(CHAT_ROLE.USER, response.getRole());
    assertEquals(CHAT_RESPONSE_TYPE.RESPONSE, response.getResponseType());
    assertEquals(12345L, response.getTimestamp());
  }

  @Test
  void mapsNullResponseType() {
    var entity =
        ChatMessageEntity.builder()
            .sessionId("session-1")
            .userId("user-1")
            .role(CHAT_ROLE.ASSISTANT)
            .contents("hello there")
            .eventStreamId("stream-1")
            .tsMillis(99999L)
            .build();

    ChatMessageResponse response = DtoMappers.mapChatEntity(entity);

    assertEquals(CHAT_ROLE.ASSISTANT, response.getRole());
    assertEquals("hello there", response.getContents());
    assertEquals(99999L, response.getTimestamp());
    assertNull(response.getResponseType());
  }
}
