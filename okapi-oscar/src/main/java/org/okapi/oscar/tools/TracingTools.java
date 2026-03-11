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
"""
  Use this tool to search for spans. When in doubt set the minimum set of filters necessary.
  Timestampfilter is always required without which the call will fail. To set timestamp filter, use the following guidelines:
  1. If the user has specified a time-interval for the investigation use that time interval.
  2. Usually a time interval of 1 hr is enough. You can use timeRange tool with the appropriate duration to get a time range that can be used to populate this filter.
  For example:
  - to find spans related to a specific service: set the ServiceFilter.service with the specific service to get spans related to a specific service.
  Refer to the descriptions of the filters to construct an appropriate set.
  If no spans are found, it is a good idea to unset a few filters i.e. relax the search criteria.
""")
  public SpanQueryV2Response getSpans(@ToolParam SpanQueryV2Request request) {
    log.info("Calling {}", request);
    var response = client.querySpans(request);
    return response;
  }

  @Tool(
      description =
          """
  Get RED metrics (Rate, Error rate, Duration) for a service. Returns aggregate RED metrics for the service, per-operation breakdowns, and per-peer-edge breakdowns.
  The only input to this service si 
  """)
  public ServiceRedResponse getServiceRedMetrics(@ToolParam ServiceRedRequest request) {
    return client.getServiceReds(request);
  }
}
