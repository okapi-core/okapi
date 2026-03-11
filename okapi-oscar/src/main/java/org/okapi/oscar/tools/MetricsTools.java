package org.okapi.oscar.tools;

import org.apache.commons.lang3.NotImplementedException;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.search.SearchMetricsRequest;
import org.okapi.rest.search.SearchMetricsResponse;
import org.okapi.rest.traces.SpanQueryV2Request;
import org.okapi.rest.traces.SpanQueryV2Response;
import org.springframework.ai.tool.annotation.ToolParam;

public class MetricsTools {
  public SearchMetricsResponse searchMetrics(@ToolParam SearchMetricsRequest request) {
    throw new NotImplementedException();
  }

  public GetMetricsResponse getMetrics(@ToolParam GetMetricsRequest request) {

    throw new NotImplementedException();
  }

  public SpanQueryV2Response getSpans(@ToolParam SpanQueryV2Request request) {

    throw new NotImplementedException();
  }
}
