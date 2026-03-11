package org.okapi.oscar.tools;

import lombok.extern.slf4j.Slf4j;
import org.okapi.ingester.client.IngesterClient;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.search.AnyMetricOrValueFilter;
import org.okapi.rest.search.SearchMetricsRequest;
import org.okapi.rest.search.SearchMetricsV2Response;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

@Slf4j
public class MetricsTools {
  private final IngesterClient client;
  private final ToolCallReporter toolCallReporter;

  public MetricsTools(IngesterClient client, ToolCallReporter toolCallReporter) {
    this.client = client;
    this.toolCallReporter = toolCallReporter;
  }

  @Tool(
      description =
"""
Purpose
Search for metric paths (name + tag combinations) emitted in a time window to discover available
metrics, tag sets, or whether a metric exists at all.
Hint
tsStartMillis and tsEndMillis MUST be in MILLISECONDS since Unix epoch; use timeRange() not
timeRangeNanos().
Example
1) Find all metrics for a service: metricNamePattern="checkout\\..*"
2) Find metrics for a specific host: anyMetricOrValueFilter.value="my-host-42"
3) Find metrics with a specific tag: valueFilters=[{label="host.name", value="my-host-42"}]
""")
  public SearchMetricsV2Response searchMetrics(@ToolParam SearchMetricsRequest request) {
    toolCallReporter.reportRequest(
        "searchMetrics", request, ToolCallSummaries.summarizeSearchMetricsRequest(request));
    var response = client.searchMetrics(request);
    toolCallReporter.reportResponse(
        "searchMetrics", ToolCallSummaries.summarizeSearchMetricsResponse(response));
    return response;
  }

  @Tool(
      description =
"""
Purpose
Discover metric paths for a specific host value.
Hint
This uses an exact value match across metric names and tags.
Example
discoverMetricsForHost("my-host-42")
""")
  public SearchMetricsV2Response discoverMetricsForHost(
      @ToolParam(description = "Host value to match exactly.") String host) {
    log.info("Discovery metrics: {}", host);
    var response = discoverMetricsFor("discoverMetricsForHost", host);
    log.info("Discovered metrics: {}", response);
    return response;
  }

  @Tool(
      description =
"""
Purpose
Discover metric paths for host values matching a pattern.
Hint
This uses an RE2 pattern match across metric names and tags.
Example
discoverMetricsForHostPattern("web-.*")
""")
  public SearchMetricsV2Response discoverMetricsForHostPattern(
      @ToolParam(description = "RE2 pattern to match host values.") String pattern) {
    return discoverMetricsForPattern("discoverMetricsForHostPattern", pattern);
  }

  @Tool(
      description =
"""
Purpose
Discover metric paths for a specific database value.
Hint
This uses an exact value match across metric names and tags.
Example
discoverMetricsForDb("orders-db")
""")
  public SearchMetricsV2Response discoverMetricsForDb(
      @ToolParam(description = "Database value to match exactly.") String db) {
    return discoverMetricsFor("discoverMetricsForDb", db);
  }

  @Tool(
      description =
"""
Purpose
Discover metric paths for database values matching a pattern.
Hint
This uses an RE2 pattern match across metric names and tags.
Example
discoverMetricsForDbPattern("orders-.*")
""")
  public SearchMetricsV2Response discoverMetricsForDbPattern(
      @ToolParam(description = "RE2 pattern to match database values.") String dbNamePattern) {
    return discoverMetricsForPattern("discoverMetricsForDbPattern", dbNamePattern);
  }

  @Tool(
      description =
          "Get metrics data. This method can return histograms, gauges and counters (or sums). Gauges have a specific resolution so its likely that we use this ")
  public GetMetricsResponse getMetrics(@ToolParam GetMetricsRequest request) {
    toolCallReporter.reportRequest(
        "getMetrics", request, ToolCallSummaries.summarizeGetMetricsRequest(request));
    var response = client.query(request);
    toolCallReporter.reportResponse(
        "getMetrics", ToolCallSummaries.summarizeGetMetricsResponse(request, response));
    return response;
  }

  private SearchMetricsV2Response discoverMetricsFor(String toolName, String value) {
    var request =
        SearchMetricsRequest.builder()
            .tsStartMillis(0)
            .tsEndMillis(System.currentTimeMillis())
            .anyMetricOrValueFilter(AnyMetricOrValueFilter.builder().value(value).build())
            .build();
    toolCallReporter.reportRequest(
        toolName, request, ToolCallSummaries.summarizeSearchMetricsRequest(request));
    var response = client.searchMetrics(request);
    toolCallReporter.reportResponse(
        toolName, ToolCallSummaries.summarizeSearchMetricsResponse(response));
    return response;
  }

  private SearchMetricsV2Response discoverMetricsForPattern(String toolName, String pattern) {
    var request =
        SearchMetricsRequest.builder()
            .tsStartMillis(0)
            .tsEndMillis(System.currentTimeMillis())
            .anyMetricOrValueFilter(AnyMetricOrValueFilter.builder().pattern(pattern).build())
            .build();
    toolCallReporter.reportRequest(
        toolName, request, ToolCallSummaries.summarizeSearchMetricsRequest(request));
    var response = client.searchMetrics(request);
    toolCallReporter.reportResponse(
        toolName, ToolCallSummaries.summarizeSearchMetricsResponse(response));
    return response;
  }
}
