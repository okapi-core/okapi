package org.okapi.oscar.service;

import lombok.AllArgsConstructor;
import org.okapi.exceptions.BadRequestException;
import org.okapi.oscar.agents.OscarResearchAgent;
import org.okapi.oscar.chat.ChatMessageEntity;
import org.okapi.oscar.chat.ChatMessageRepository;
import org.okapi.oscar.inference.InferenceJob;
import org.okapi.oscar.inference.OscarInferenceJobPool;
import org.okapi.oscar.session.*;
import org.okapi.rest.chat.*;
import org.okapi.rest.session.SESSION_STATE;
import org.okapi.rest.session.STREAM_STATE;
import org.okapi.rest.session.SessionMetaResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OscarAi {

  private final OscarResearchAgent researchAgent;
  private final ChatMessageRepository chatMessageRepository;
  private final OscarInferenceJobPool jobPool;
  private final SessionMetaRepository sessionMetaRepository;
  private final StreamMetaRepository streamMetaRepository;
  private final StreamStateChecker checker;

  public ChatResponse postMessage(String sessionId, PostMessageRequest request) {
    var session =
        sessionMetaRepository
            .findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

    var ongoingStream = session.getOngoingStream();
    if (ongoingStream != null && ongoingStream.getState() == STREAM_STATE.OPEN) {
      throw new BadRequestException("A previous message is already being processed");
    }

    var stream =
        streamMetaRepository.save(
            StreamMetaEntity.builder()
                .sessionId(sessionId)
                .startTime(System.currentTimeMillis())
                .state(STREAM_STATE.OPEN)
                .build());

    sessionMetaRepository.updateOngoingStream(sessionId, stream);

    chatMessageRepository.save(
        ChatMessageEntity.builder()
            .sessionId(sessionId)
            .userId(request.getUserId())
            .role(CHAT_ROLE.USER)
            .contents(request.getMessage())
            .eventStreamId(String.valueOf(stream.getStreamId()))
            .responseType(CHAT_RESPONSE_TYPE.MARKDOWN_TEXT)
            .tsMillis(System.currentTimeMillis())
            .build());

    var job = makeJob(sessionId, String.valueOf(stream.getStreamId()), request);
    jobPool.submit(job);
    return ChatResponse.builder()
        .sessionId(sessionId)
        .streamId(String.valueOf(stream.getStreamId()))
        .build();
  }

  public InferenceJob makeJob(String sessionId, String streamId, PostMessageRequest request) {
    return new InferenceJob(
        sessionId,
        () -> {
          researchAgent.respond(sessionId, streamId, request.getMessage());
          return null;
        },
        checker.makeSessionInactiveChecker(sessionId));
  }

  public ChatMessageUpdatesResponse getUpdates(String sessionId) {
    var session =
        sessionMetaRepository
            .findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    if (session.getOngoingStream() == null) {
      return ChatMessageUpdatesResponse.builder()
          .messages(List.of())
          .streamState(STREAM_STATE.CLOSED)
          .build();
    }
    var stream = session.getOngoingStream();
    var streamId = String.valueOf(stream.getStreamId());
    List<ChatMessageResponse> messages =
        chatMessageRepository
            .findBySessionIdAndEventStreamIdOrderByTsMillisAsc(sessionId, streamId)
            .stream()
            .map(DtoMappers::mapChatEntity)
            .toList();
    return ChatMessageUpdatesResponse.builder()
        .messages(messages)
        .streamState(stream.getState())
        .build();
  }

  public SessionMetaResponse createSession() {
    long now = System.currentTimeMillis();
    var entity =
        SessionMetaEntity.builder()
            .sessionId(UUID.randomUUID().toString())
            .state(SESSION_STATE.OPEN)
            .startTime(now)
            .lastRecordedPing(now)
            .build();
    var saved = sessionMetaRepository.save(entity);
    return toSessionMetaResponse(saved);
  }

  public SessionMetaResponse getSessionMeta(String sessionId) {
    var entity =
        sessionMetaRepository
            .findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    return toSessionMetaResponse(entity);
  }

  private SessionMetaResponse toSessionMetaResponse(SessionMetaEntity entity) {
    return SessionMetaResponse.builder()
        .sessionId(entity.getSessionId())
        .ongoingStreamId(
            entity.getOngoingStream() != null ? entity.getOngoingStream().getStreamId() : null)
        .lastRecordedPing(entity.getLastRecordedPing())
        .state(entity.getState())
        .build();
  }

  public SessionMetaResponse pingSession(String sessionId) {
    sessionMetaRepository.updateLastRecordedPing(sessionId, System.currentTimeMillis());
    return getSessionMeta(sessionId);
  }

  public ChatHistoryResponse getHistory(String sessionId, Long from, Long to) {
    var request = GetHistoryRequest.builder().from(from).to(to).build();
    return getHistory(sessionId, request);
  }

  public ChatHistoryResponse getHistory(String sessionId, GetHistoryRequest request) {
    long toTs = request.getTo() != null ? request.getTo() : System.currentTimeMillis();
    List<ChatMessageEntity> messages =
        chatMessageRepository.findBySessionIdAndTsMillisBetweenOrderByTsMillisAsc(
            sessionId, request.getFrom(), toTs);
    List<ChatMessageResponse> responses = messages.stream().map(DtoMappers::mapChatEntity).toList();
    return ChatHistoryResponse.builder().responses(responses).build();
  }
}
