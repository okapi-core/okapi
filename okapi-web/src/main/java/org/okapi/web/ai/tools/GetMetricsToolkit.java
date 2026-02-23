package org.okapi.web.ai.tools;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.okapi.web.ai.tools.params.TimeSeriesQuery;
import org.okapi.web.ai.tools.signals.GaugeSignal;
import org.okapi.web.ai.tools.signals.HistoSignal;

public interface GetMetricsToolkit extends AiToolkit {
  ToolCallResult<GaugeSignal> getTimeSeries(QueryContext context, TimeSeriesQuery query)
      throws ExecutionException, InterruptedException, TimeoutException;

  ToolCallResult<HistoSignal> getHistogram(QueryContext context, TimeSeriesQuery query)
      throws ExecutionException, InterruptedException, TimeoutException;
}
