package org.okapi.oscar.tools;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.ingester.client.IngesterClient;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.search.SearchMetricsRequest;
import org.okapi.rest.search.SearchMetricsResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class MetricsTools {
  IngesterClient client;

  @Tool(
      description =
          "Search metrics which match provided filters. Use this tool to find metrics which match a pattern that have been emitted in the provided time window." +
                  "Use this to find all tags related to a specific metric. The tags can be used to refine search if a query returns no response.")
  public SearchMetricsResponse searchMetrics(@ToolParam SearchMetricsRequest request) {
    log.info("req: {}", request);
    var response = client.searchMetrics(request);
    log.info("res: {}", response);
    return response;
  }

  @Tool(
      description =
          "Get metrics data. This method can return histograms, gauges and counters (or sums). Gauges have a specific resolution so its likely that we use this ")
  public GetMetricsResponse getMetrics(@ToolParam GetMetricsRequest request) {
    log.info("req: {}", request);
    var response = client.query(request);
    log.info("res: {}", response);
    return response;
  }
}
