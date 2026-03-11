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
          "Search for metric paths (name + tag combinations) that were emitted in a time window."
              + " Use this to discover which metrics exist, find all tag combinations for a metric,"
              + " or check if a metric is being emitted at all."
              + " tsStartMillis and tsEndMillis MUST be in MILLISECONDS since Unix epoch —"
              + " use timeRange() not timeRangeNanos() to populate them."
              + " Example 1 — find all metrics for a service: set metricNamePattern to a RE2 regex"
              + " (e.g. 'checkout\\..*') to match all metrics whose names start with that prefix."
              + " Example 2 — find metrics for a specific host: set anyMetricOrValueFilter.value to"
              + " the exact host name tag value (e.g. 'my-host-42') to match any metric whose"
              + " name or any tag value equals that string exactly."
              + " Example 3 — find metrics with a specific tag: set valueFilters with the label"
              + " and value (e.g. label='host.name', value='my-host-42')."
              + " NOTE: anyMetricOrValueFilter.value performs an EXACT match — do NOT use it for"
              + " service name prefixes like 'checkout'; use metricNamePattern instead.")
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
