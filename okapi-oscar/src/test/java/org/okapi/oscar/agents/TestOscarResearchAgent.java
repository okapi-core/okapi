package org.okapi.oscar.agents;

import org.okapi.oscar.agent.TestOscarAgent;
import org.okapi.oscar.spring.cfg.OkapiOscarCfg;
import org.okapi.oscar.tools.DateTimeTools;
import org.okapi.oscar.tools.FilterContributionTool;
import org.okapi.oscar.tools.GreetingTools;
import org.okapi.oscar.tools.MetricsTools;
import org.okapi.oscar.tools.StatefulToolFactory;
import org.okapi.oscar.tools.StatefulTools;
import org.okapi.oscar.tools.TracingTools;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;

import java.util.concurrent.CompletableFuture;

public class TestOscarResearchAgent extends OscarResearchAgent {

  private final StatefulToolFactory statefulToolFactory;

  public TestOscarResearchAgent(
      OpenAiChatModel chatModel,
      ChatMemory chatMemory,
      OkapiOscarCfg cfg,
      MetricsTools metricsTools,
      TracingTools tracingTools,
      DateTimeTools dateTimeTools,
      GreetingTools greetingTools,
      FilterContributionTool filterContributionTool,
      StatefulToolFactory statefulToolFactory) {
    super(
        chatModel,
        chatMemory,
        cfg,
        metricsTools,
        tracingTools,
        dateTimeTools,
        greetingTools,
        filterContributionTool,
        statefulToolFactory);
    this.statefulToolFactory = statefulToolFactory;
  }

  @Override
  public void respond(String sessionId, long streamId, String userMessage) {
    var tools = statefulToolFactory.getTools(sessionId, streamId);
    CompletableFuture.runAsync(() -> writeResponse(tools, userMessage)).join();
  }

  private void writeResponse(StatefulTools tools, String userMessage) {
    try {
      if (isSecondRound(userMessage)) {
        writeSecondChunk(tools);
      } else {
        writeFirstChunk(tools);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("TestOscarResearchAgent interrupted", e);
    }
  }

  private boolean isSecondRound(String userMessage) {
    return userMessage != null && userMessage.toLowerCase().contains("second");
  }

  private void writeFirstChunk(StatefulTools tools) throws InterruptedException {
    tools.postThought("Analyzing the first request");
    Thread.sleep(100);
    tools.postPlan("Step 1: retrieve data. Step 2: format response.");
    Thread.sleep(100);
    tools.postResponse(TestOscarAgent.FIRST_CHUNK_RESPONSE);
  }

  private void writeSecondChunk(StatefulTools tools) throws InterruptedException {
    tools.postThought("Analyzing the second request");
    Thread.sleep(100);
    tools.postResponse(TestOscarAgent.SECOND_CHUNK_RESPONSE);
  }
}
