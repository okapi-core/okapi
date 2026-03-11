package org.okapi.oscar.tools;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.oscar.spring.cfg.OkapiOscarGreetingCfg;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@AllArgsConstructor
public class GreetingTools {
  private static final String DEFAULT_GREETING = "Hello!";

  private final OkapiOscarGreetingCfg cfg;

  @Tool(
      description =
          """
  Return a random greeting from a preconfigured list.
  Use this tool whenever the user submits a greeting.
  The return value of this tool should be your response.
  Note users always expect a response.
  """)
  public String randomGreeting() {
    List<String> greetings = cfg.getGreetings();
    if (greetings == null || greetings.isEmpty()) {
      log.warn("No greetings configured; using default greeting.");
      return DEFAULT_GREETING;
    }
    int idx = ThreadLocalRandom.current().nextInt(greetings.size());
    return greetings.get(idx);
  }
}
