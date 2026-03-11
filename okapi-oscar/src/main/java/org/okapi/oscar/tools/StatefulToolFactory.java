package org.okapi.oscar.tools;

import lombok.AllArgsConstructor;
import org.okapi.ingester.client.IngesterClient;
import org.okapi.oscar.chat.ChatMessageRepository;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class StatefulToolFactory {

  private final ChatMessageRepository repository;
  private final IngesterClient ingesterClient;

  public StatefulToolContext getTools(String sessionId, long streamId) {
    var statefulTools = StatefulTools.forSessionAndStream(sessionId, streamId, repository);
    var reporter = new ToolCallReporter(sessionId, streamId, statefulTools);
    var metricsTools = new MetricsTools(ingesterClient, reporter);
    var tracingTools = new TracingTools(ingesterClient, reporter);
    return new StatefulToolContext(statefulTools, metricsTools, tracingTools);
  }
}
