/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.backends.datadog;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.okapi.agent.dto.JOB_STATUS;
import org.okapi.agent.dto.PendingJob;
import org.okapi.agent.dto.QueryResult;
import org.okapi.web.ai.tools.*;
import org.okapi.web.ai.tools.params.TimeSeriesQuery;
import org.okapi.web.ai.tools.signals.GaugeSignal;
import org.okapi.web.ai.tools.signals.HistoSignal;
import org.okapi.web.service.federation.dispatcher.JobDispatcher;

public class DataDogGetMetricsToolkit implements GetMetricsToolkit {
  JobDispatcher dispatcher;
  DatadogQueryWriter datadogQueryWriter;
  ToolConfig toolConfig;

  public DataDogGetMetricsToolkit(
      JobDispatcher dispatcher, DatadogQueryWriter datadogQueryWriter, ToolConfig toolConfig) {
    this.dispatcher = dispatcher;
    this.datadogQueryWriter = datadogQueryWriter;
    this.toolConfig = toolConfig;
  }

  @Override
  public ToolCallResult<GaugeSignal> getTimeSeries(QueryContext context, TimeSeriesQuery query)
      throws ExecutionException, InterruptedException, TimeoutException {
    var queryId = UUID.randomUUID().toString();
    var ddgQuery = datadogQueryWriter.writeGetTsQuery(query);
    var pendingJob =
        PendingJob.builder()
            .jobId(queryId)
            .jobStatus(JOB_STATUS.PENDING)
            .sourceId(context.sourceId())
            .spec(ddgQuery)
            .build();
    var future = dispatcher.dispatchJob(context.getSession().orgId(), pendingJob);
    var result = future.get(toolConfig.timeoutMillis(), TimeUnit.MILLISECONDS);
    handleQueryError(result);

    var decoded = DatadogMetricsDecoder.mapTimeSeriesResult(result.data());
    StringBuilder errorBuilder = new StringBuilder();
    if (!decoded.isSuccess()) {
      errorBuilder.append(decoded.getErrorMessage());
    }
    if (result.processingError() != null) {
      if (!errorBuilder.isEmpty()) {
        errorBuilder.append("; ");
      }
      errorBuilder.append(result.processingError());
    }

    if (errorBuilder.isEmpty()) {
      return new ToolCallResult<>(decoded.getResult(), true, null);
    }
    return new ToolCallResult<>(null, false, errorBuilder.toString());
  }

  @Override
  public ToolCallResult<HistoSignal> getHistogram(QueryContext context, TimeSeriesQuery query)
      throws ExecutionException, InterruptedException, TimeoutException {
    var queryId = UUID.randomUUID().toString();
    var ddgQuery = datadogQueryWriter.writeGetDistributionQuery(query);
    var pendingJob =
        PendingJob.builder()
            .jobId(queryId)
            .jobStatus(JOB_STATUS.PENDING)
            .sourceId(context.sourceId())
            .spec(ddgQuery)
            .build();
    var future = dispatcher.dispatchJob(context.getSession().orgId(), pendingJob);
    var result = future.get(toolConfig.timeoutMillis(), TimeUnit.MILLISECONDS);
    handleQueryError(result);

    var decoded = DatadogMetricsDecoder.mapHistoSignal(result.data());
    StringBuilder errorBuilder = new StringBuilder();
    if (!decoded.isSuccess()) {
      errorBuilder.append(decoded.getErrorMessage());
    }
    if (result.processingError() != null) {
      if (!errorBuilder.isEmpty()) {
        errorBuilder.append("; ");
      }
      errorBuilder.append(result.processingError());
    }

    if (errorBuilder.isEmpty()) {
      return new ToolCallResult<>(decoded.getResult(), true, null);
    }
    return new ToolCallResult<>(null, false, errorBuilder.toString());
  }

  private void handleQueryError(QueryResult result) throws ExecutionException {
    if (result.error() != null) {
      throw new ExecutionException(result.error(), null);
    }
  }
}
