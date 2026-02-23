package org.okapi.web.ai.tools.impl;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.okapi.web.ai.tools.GetTracesToolkit;
import org.okapi.web.ai.tools.QueryContext;
import org.okapi.web.ai.tools.params.SpanQuery;

public class GetTracesTool {
  QueryContext queryContext;
  GetTracesToolkit toolkit;

  @Tool("Fetch traces from the tracing store based on the provided span query.")
  public String getTraces(
      @P(
"""
Object with fields:
- startTime: start time in milliseconds since epoch (long)
- endTime: end time in milliseconds since epoch (long)
- service: name of the service (string)
- traceId: (optional) trace ID to filter by (string)
- labelFilter: (optional) label filter to apply. This is a map of label keys to label values (map of string to string). Label values are not treated as regex.
""")
          SpanQuery spanQuery)
      throws ExecutionException, InterruptedException, TimeoutException {
    return toolkit.getSpans(queryContext, spanQuery).toString();
  }
}
