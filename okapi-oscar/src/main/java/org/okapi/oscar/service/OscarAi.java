package org.okapi.oscar.service;

import java.util.List;
import lombok.AllArgsConstructor;
import org.okapi.oscar.agents.OscarResearchAgent;
import org.okapi.oscar.chat.ChatMessageEntity;
import org.okapi.oscar.chat.ChatMessageRepository;
import org.okapi.rest.chat.CHAT_RESPONSE_TYPE;
import org.okapi.rest.chat.CHAT_ROLE;
import org.okapi.rest.chat.ChatHistoryResponse;
import org.okapi.rest.chat.ChatResponse;
import org.okapi.rest.chat.PostMessageRequest;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OscarAi {

  private final OscarResearchAgent researchAgent;
  private final ChatMessageRepository chatMessageRepository;

  public ChatResponse postMessage(String sessionId, PostMessageRequest request) {
    chatMessageRepository.save(
        ChatMessageEntity.builder()
            .sessionId(sessionId)
            .userId(request.getUserId())
            .role(CHAT_ROLE.USER)
            .contents(request.getMessage())
            .responseType(CHAT_RESPONSE_TYPE.MARKDOWN_TEXT)
            .tsMillis(System.currentTimeMillis())
            .build());

    String aiResponse = researchAgent.respond(sessionId, request.getMessage());

    long ts = System.currentTimeMillis();
    chatMessageRepository.save(
        ChatMessageEntity.builder()
            .sessionId(sessionId)
            .userId(request.getUserId())
            .role(CHAT_ROLE.ASSISTANT)
            .contents(aiResponse)
            .responseType(CHAT_RESPONSE_TYPE.MARKDOWN_TEXT)
            .tsMillis(ts)
            .build());

    return ChatResponse.builder()
        .timestamp(String.valueOf(ts))
        .contents(aiResponse)
        .responseType(CHAT_RESPONSE_TYPE.MARKDOWN_TEXT)
        .role(CHAT_ROLE.ASSISTANT)
        .build();
  }

  public ChatHistoryResponse getHistory(String sessionId, Long from, Long to) {
    long toTs = to != null ? to : System.currentTimeMillis();
    List<ChatMessageEntity> messages =
        chatMessageRepository.findBySessionIdAndTsMillisBetweenOrderByTsMillisAsc(
            sessionId, from, toTs);
    List<ChatResponse> responses =
        messages.stream()
            .map(
                m ->
                    ChatResponse.builder()
                        .timestamp(String.valueOf(m.getTsMillis()))
                        .contents(m.getContents())
                        .responseType(m.getResponseType())
                        .role(m.getRole())
                        .build())
            .toList();
    return ChatHistoryResponse.builder().responses(responses).build();
  }
}
