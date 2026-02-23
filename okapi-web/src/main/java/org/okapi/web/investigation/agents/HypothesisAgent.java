/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.agents;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

public class HypothesisAgent {
  String modelName;
  String apiKey;
  InvestigationToolkit toolkit;

  public String investigate(String issueDescription) {
    var memory = new InMemoryChatMemoryStore();
    var template =
"""
You are an SRE. You are provided with a set of tools. Use the right tools to collect data.
        """;
    var model =
        OpenAiChatModel.builder().modelName(modelName).apiKey(apiKey).user(template).build();
    var issueTemplate =
"""
Issue Description:
"""
            + issueDescription
            + "\n"
            +
"""
Please provide an investigation plan to diagnose the issue.
""";
    var output =
        model.doChat(
            ChatRequest.builder()
                .messages(UserMessage.from(issueTemplate))
                .toolSpecifications(toolkit.getTools())
                .build());
    return output.aiMessage().text();
  }
}
