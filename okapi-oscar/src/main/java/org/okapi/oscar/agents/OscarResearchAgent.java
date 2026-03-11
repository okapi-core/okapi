package org.okapi.oscar.agents;

import lombok.extern.slf4j.Slf4j;
import org.okapi.oscar.spring.cfg.OkapiOscarCfg;
import org.okapi.oscar.tools.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OscarResearchAgent {

  private final ChatClient chatClient;
  private final OkapiOscarCfg cfg;
  private final MetricsTools metricsTools;
  private final TracingTools tracingTools;
  private final DateTimeTools dateTimeTools;
  private final GreetingTools greetingTools;
  private final FilterContributionTool filterContributionTool;
  private final StatefulToolFactory statefulToolFactory;

  public OscarResearchAgent(
      OpenAiChatModel chatModel,
      ChatMemory chatMemory,
      OkapiOscarCfg cfg,
      MetricsTools metricsTools,
      TracingTools tracingTools,
      DateTimeTools dateTimeTools,
      GreetingTools greetingTools,
      FilterContributionTool filterContributionTool,
      StatefulToolFactory statefulToolFactory) {
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
    this.cfg = cfg;
    this.metricsTools = metricsTools;
    this.tracingTools = tracingTools;
    this.dateTimeTools = dateTimeTools;
    this.greetingTools = greetingTools;
    this.statefulToolFactory = statefulToolFactory;
    this.filterContributionTool = filterContributionTool;
  }

  public void respond(String sessionId, long streamId, String userMessage) {
    chatClient
        .prompt()
        .system(cfg.getSystemPrompt())
        .user(userMessage)
        .tools(
            metricsTools,
            tracingTools,
            dateTimeTools,
            greetingTools,
            filterContributionTool,
            statefulToolFactory.getTools(sessionId, streamId))
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
        .call()
        .content();
  }
}
