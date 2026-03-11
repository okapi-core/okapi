package org.okapi.oscar.integ.judge;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class JudgeAgent {

  private final ChatClient chatClient;

  public JudgeAgent(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  public Judgment judge(String question, String expectedAnswer, String actualAnswer) {
    String prompt =
        """
        You are a response quality judge. Assess how well the actual answer matches the expected answer given the question.

        Return exactly one word — no explanation, no punctuation:
        - EXACT: the actual answer conveys the same key information as the expected answer
        - ALMOST: mostly correct but missing details or has minor inaccuracies
        - WRONG: incorrect, irrelevant, or fails to address the question

        Question: %s
        Expected: %s
        Actual: %s

        Respond with exactly one word: EXACT, ALMOST, or WRONG.
        """
            .formatted(question, expectedAnswer, actualAnswer);

    String raw = chatClient.prompt().user(prompt).call().content().trim().toUpperCase();
    for (Judgment j : Judgment.values()) {
      if (raw.contains(j.name())) return j;
    }
    return Judgment.WRONG;
  }
}
