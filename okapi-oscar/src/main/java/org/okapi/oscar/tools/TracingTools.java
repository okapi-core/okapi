package org.okapi.oscar.tools;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.ingester.client.IngesterClient;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.rest.traces.SpanQueryV2Request;
import org.okapi.rest.traces.SpanQueryV2Response;
import org.okapi.rest.traces.TimestampFilter;
import org.okapi.rest.traces.red.ServiceRedRequest;
import org.okapi.rest.traces.red.ServiceRedResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

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
    log.info("Response: {}", response);
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

  @Tool(
      description =
"""
Purpose
Discover downstream peer services for a given service over a time window.
Hint
Uses this to set `peer` when setting a service filter to get spans.
Example
discoverPeers("checkout-service", 1700000000000000000, 1700003600000000000)
""")
  public List<String> discoverPeers(
      @ToolParam(description = "Service name to inspect.") String service,
      @ToolParam(description = "Start timestamp in nanoseconds since Unix epoch.") long startNs,
      @ToolParam(description = "End timestamp in nanoseconds since Unix epoch.") long endNs) {
    var response = getServiceReds(service, startNs, endNs);
    if (response.getPeerReds() == null) {
      return Collections.emptyList();
    }
    return response.getPeerReds().stream()
        .map(peer -> peer == null ? null : peer.getPeerService())
        .filter(name -> name != null && !name.isBlank())
        .toList();
  }

  private ServiceRedResponse getServiceReds(String service, long startNs, long endNs) {
    var request =
        ServiceRedRequest.builder()
            .service(service)
            .timestampFilter(
                TimestampFilter.builder().tsStartNanos(startNs).tsEndNanos(endNs).build())
            .resType(RES_TYPE.MINUTELY)
            .build();
    return client.getServiceReds(request);
  }
}
