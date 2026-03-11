package org.okapi.oscar.tools;

import com.google.gson.Gson;
import org.okapi.oscar.chat.ChatMessageEntity;
import org.okapi.oscar.chat.ChatMessageRepository;
import org.okapi.rest.chat.CHAT_RESPONSE_TYPE;
import org.okapi.rest.chat.CHAT_ROLE;
import org.okapi.rest.chat.payload.GetTraceFollowUpPayload;
import org.okapi.rest.chat.payload.PlotMetricFollowUpPayload;
import org.okapi.rest.chat.payload.PostPlanPayload;
import org.okapi.rest.chat.payload.PostResponsePayload;
import org.okapi.rest.chat.payload.PostThoughtPayload;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class StatefulTools {

  private static final Gson GSON = new Gson();
  private static final String AGENT_USER_ID = "assistant";

  private final String sessionId;
  private final ChatMessageRepository repository;
  private final String streamId;

  StatefulTools(String sessionId, String streamId, ChatMessageRepository repository) {
    this.sessionId = sessionId;
    this.repository = repository;
    this.streamId = streamId;
  }

  @Tool(
      description =
          "Record the agent's internal reasoning step. Call this to log thinking before taking actions.")
  public void postThought(
      @ToolParam(description = "The agent's reasoning or thinking.") String thought) {
    persist(CHAT_RESPONSE_TYPE.THOUGHT, GSON.toJson(new PostThoughtPayload(thought)));
  }

  @Tool(
      description =
          "Record the investigation plan. Call this once at the start, before executing any tool calls.")
  public void postPlan(
      @ToolParam(description = "The step-by-step investigation plan.") String plan) {
    persist(CHAT_RESPONSE_TYPE.PLAN, GSON.toJson(new PostPlanPayload(plan)));
  }

  @Tool(
      description =
          "Send a response to the user. This is the only way to communicate findings to the user. Call this instead of returning free text.")
  public void postResponse(
      @ToolParam(description = "The response to send to the user.") String response) {
    persist(CHAT_RESPONSE_TYPE.RESPONSE, GSON.toJson(new PostResponsePayload(response)));
  }

  @Tool(description = "Recommend a trace follow-up action to the user for a specific trace ID.")
  public void postGetTraceFollowUp(
      @ToolParam(description = "The trace ID the user should investigate further.")
          String traceId) {
    persist(
        CHAT_RESPONSE_TYPE.GET_TRACE_FOLLOW_UP, GSON.toJson(new GetTraceFollowUpPayload(traceId)));
  }

  @Tool(
      description =
          "Recommend a metric plot follow-up action to the user for a specific metric query.")
  public void postPlotMetricFollowUp(
      @ToolParam(description = "The metric query the user should plot.")
          GetMetricsRequest request) {
    persist(
        CHAT_RESPONSE_TYPE.PLOT_METRIC_FOLLOW_UP,
        GSON.toJson(new PlotMetricFollowUpPayload(request)));
  }

  private void persist(CHAT_RESPONSE_TYPE responseType, String contents) {
    repository.save(
        ChatMessageEntity.builder()
            .sessionId(sessionId)
            .userId(AGENT_USER_ID)
            .role(CHAT_ROLE.ASSISTANT)
            .responseType(responseType)
            .eventStreamId(streamId)
            .contents(contents)
            .tsMillis(System.currentTimeMillis())
            .build());
  }

  public static StatefulTools forSessionAndStream(
      String sessionId, String streamId, ChatMessageRepository repository) {
    return new StatefulTools(sessionId, streamId, repository);
  }
}
