package org.okapi.oscar.agents;

import org.okapi.oscar.spring.cfg.OkapiOscarCfg;
import org.okapi.oscar.tools.MetricsTools;
import org.okapi.oscar.tools.TracingTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

@Component
public class OscarResearchAgent {

  private final ChatClient chatClient;
  private final OkapiOscarCfg cfg;
  private final MetricsTools metricsTools;
  private final TracingTools tracingTools;

  public OscarResearchAgent(
      OpenAiChatModel chatModel,
      ChatMemory chatMemory,
      OkapiOscarCfg cfg,
      MetricsTools metricsTools,
      TracingTools tracingTools) {
    this.chatClient =
        ChatClient.builder(chatModel)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
    this.cfg = cfg;
    this.metricsTools = metricsTools;
    this.tracingTools = tracingTools;
  }

  public String respond(String sessionId, String userMessage) {
    return chatClient
        .prompt()
        .system(cfg.getSystemPrompt())
        .user(userMessage)
        .tools(metricsTools, tracingTools)
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
        .call()
        .content();
  }
}
