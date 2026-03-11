package org.okapi.oscar.tools;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.ingester.client.IngesterClient;
import org.okapi.rest.traces.SpanQueryV2Request;
import org.okapi.rest.traces.SpanQueryV2Response;
import org.okapi.rest.traces.red.ServiceRedRequest;
import org.okapi.rest.traces.red.ServiceRedResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class TracingTools {

  IngesterClient client;

  @Tool(
      description =
          "Search for spans matching the provided filters. Use this tool to find traces and spans by service, time window, HTTP attributes, database attributes, duration, or custom span attributes."
              + "When in doubt set the minimum set of filters necessary. Setting more filters than necessary could lead to no results being returned.")
  public SpanQueryV2Response getSpans(@ToolParam SpanQueryV2Request request) {
    var response = client.querySpans(request);
    return response;
  }

  @Tool(
      description =
          "Get RED metrics (Rate, Error rate, Duration) for a service. Returns aggregate RED metrics for the service, per-operation breakdowns, and per-peer-edge breakdowns.")
  public ServiceRedResponse getServiceRedMetrics(@ToolParam ServiceRedRequest request) {
    return client.getServiceReds(request);
  }
}
