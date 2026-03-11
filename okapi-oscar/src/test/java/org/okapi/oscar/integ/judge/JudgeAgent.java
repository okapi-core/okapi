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
        You are a strict response quality judge. Your job is to verify that the actual answer contains the specific facts stated in the expected answer.

        Scoring rules — apply them in order, stop at the first match:
        - EXACT: the actual answer explicitly states every key fact in the expected answer.
        - ALMOST: the actual answer contains most key facts but is missing one minor detail or uses slightly imprecise wording.
        - WRONG: the actual answer is missing one or more key facts from the expected answer, reaches a different conclusion, expresses uncertainty where the expected answer is definitive, or only partially addresses the question.

        Important:
        - A response that says "I could not find X" or "there are no results" when the expected answer asserts X was found MUST be WRONG.
        - Do not give credit for adjacent or related facts — only credit explicit matches to what the expected answer states.
        - When in doubt, return WRONG.

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
