package org.okapi.oscar.agents;

import lombok.extern.slf4j.Slf4j;
import org.okapi.oscar.spring.cfg.OkapiOscarCfg;
import org.okapi.oscar.tools.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!dummy")
public class OscarResearchAgent implements SreResearchAgent {

  private final ChatClient chatClient;
  private final OkapiOscarCfg cfg;
  private final DateTimeTools dateTimeTools;
  private final GreetingTools greetingTools;
  private final FilterContributionTool filterContributionTool;
  private final StatefulToolFactory statefulToolFactory;

  public OscarResearchAgent(
      OpenAiChatModel chatModel,
      ChatMemory chatMemory,
      OkapiOscarCfg cfg,
      DateTimeTools dateTimeTools,
      GreetingTools greetingTools,
      FilterContributionTool filterContributionTool,
      StatefulToolFactory statefulToolFactory) {
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
    this.cfg = cfg;
    this.dateTimeTools = dateTimeTools;
    this.greetingTools = greetingTools;
    this.statefulToolFactory = statefulToolFactory;
    this.filterContributionTool = filterContributionTool;
  }

  @Override
  public void respond(String sessionId, long streamId, String userMessage) {
    var toolContext = statefulToolFactory.getTools(sessionId, streamId);
    chatClient
        .prompt()
        .system(cfg.getSystemPrompt())
        .user(userMessage)
        .tools(
            toolContext.getMetricsTools(),
            toolContext.getTracingTools(),
            dateTimeTools,
            greetingTools,
            filterContributionTool,
            toolContext.getStatefulTools())
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
        .call()
        .content();
  }
}
