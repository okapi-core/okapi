package org.okapi.oscar.tools;

import lombok.AllArgsConstructor;
import org.okapi.ingester.client.IngesterClient;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.search.SearchMetricsRequest;
import org.okapi.rest.search.SearchMetricsResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MetricsTools {
  IngesterClient client;

  @Tool(
      description =
          "Search metrics which match provided filters. Use this tool to find metrics which match a pattern that have been emitted in the provided time window.")
  public SearchMetricsResponse searchMetrics(@ToolParam SearchMetricsRequest request) {
    return client.searchMetrics(request);
  }

  @Tool(
      description =
          "Get metrics data. This method can return histograms, gauges and counters (or sums). Gauges have a specific resolution so its likely that we use this ")
  public GetMetricsResponse getMetrics(@ToolParam GetMetricsRequest request) {
    return client.query(request);
  }
}
