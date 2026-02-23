/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.backends.prometheus;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.okapi.web.ai.tools.*;
import org.okapi.web.ai.tools.params.TimeSeriesQuery;
import org.okapi.web.ai.tools.signals.GaugeSignal;
import org.okapi.web.ai.tools.signals.HistoSignal;
import org.okapi.web.service.federation.dispatcher.JobDispatcher;

public class PrometheusGetMetricsToolkit implements GetMetricsToolkit {

  JobDispatcher dispatcher;
  PromQlQueryWriter promQlQueryWriter;
  PrometheusToolConfig toolConfig;

  @Override
  public ToolCallResult<GaugeSignal> getTimeSeries(QueryContext context, TimeSeriesQuery query)
      throws ExecutionException, InterruptedException, TimeoutException {
    return null;
  }

  @Override
  public ToolCallResult<HistoSignal> getHistogram(QueryContext context, TimeSeriesQuery query)
      throws ExecutionException, InterruptedException, TimeoutException {
    return null;
  }
}
