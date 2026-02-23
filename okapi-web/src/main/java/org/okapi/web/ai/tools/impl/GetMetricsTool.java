/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.impl;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.okapi.web.ai.tools.GetMetricsToolkit;
import org.okapi.web.ai.tools.QueryContext;
import org.okapi.web.ai.tools.params.TimeSeriesQuery;

public class GetMetricsTool {
  QueryContext context;
  private GetMetricsToolkit toolkit;

  public GetMetricsTool(GetMetricsToolkit toolkit) {
    this.toolkit = toolkit;
  }

  @Tool("Fetch gauge metrics from the metrics store based on the provided query.")
  public String getTimeSeries(
      @P(
"""
Object with fields:
- pathName: a metric path name (string)
- tags: a map of tag keys to tag values (map of string to string)
- startTime: start time in milliseconds since epoch (long)
- endTime: end time in milliseconds since epoch (long)
- resolution: (optional) resolution in milliseconds (long)
""")
          TimeSeriesQuery timeSeriesQuery)
      throws ExecutionException, InterruptedException, TimeoutException {
    var signal = toolkit.getTimeSeries(context, timeSeriesQuery);
    return signal.toString();
  }

  @Tool("Fetch histogram metrics from the metrics store based on the provided query.")
  public String getHistogram(
      @P(
"""
Object with fields:
- pathName: a metric path name (string)
- tags: a map of tag keys to tag values (map of string to string)
- startTime: start time in milliseconds since epoch (long)
- endTime: end time in milliseconds since epoch (long)
- resolution: (optional) resolution in milliseconds (long)
""")
          TimeSeriesQuery timeSeriesQuery)
      throws ExecutionException, InterruptedException, TimeoutException {
    return toolkit.getHistogram(context, timeSeriesQuery).toString();
  }
}
