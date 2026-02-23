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
import org.okapi.web.ai.tools.GetTracesToolkit;
import org.okapi.web.ai.tools.QueryContext;
import org.okapi.web.ai.tools.ToolCallResult;
import org.okapi.web.ai.tools.params.SpanQuery;
import org.okapi.web.ai.tools.signals.TracesSignal;
import org.okapi.web.service.federation.dispatcher.JobDispatcher;

public class DataDogGetSpansToolkit implements GetTracesToolkit {

  JobDispatcher dispatcher;
  DatadogQueryWriter datadogQueryWriter;
  ToolConfig toolConfig;

  public DataDogGetSpansToolkit(
      JobDispatcher dispatcher, DatadogQueryWriter datadogQueryWriter, ToolConfig toolConfig) {
    this.dispatcher = dispatcher;
    this.datadogQueryWriter = datadogQueryWriter;
    this.toolConfig = toolConfig;
  }

  @Override
  public ToolCallResult<TracesSignal> getSpans(QueryContext context, SpanQuery spanQuery)
      throws ExecutionException, InterruptedException, TimeoutException {
    var queryId = UUID.randomUUID().toString();
    var spec = datadogQueryWriter.writeGetSpanQuery(spanQuery);
    var pendingJob =
        PendingJob.builder()
            .jobId(queryId)
            .jobStatus(JOB_STATUS.PENDING)
            .sourceId(context.sourceId())
            .spec(spec)
            .build();
    var future = dispatcher.dispatchJob(context.getSession().orgId(), pendingJob);
    var result = future.get(toolConfig.timeoutMillis(), TimeUnit.MILLISECONDS);
    if (result.error() != null) {
      throw new RuntimeException(result.error());
    }
    var decoded = DatadogTracesDecoder.mapToSpans(result.data());

    var sb = new StringBuilder();
    if (!decoded.isSuccess()) {
      sb.append(decoded.getErrorMessage());
    }
    if (result.processingError() != null) {
      if (!sb.isEmpty()) {
        sb.append("; ");
      }
      sb.append(result.processingError());
    }
    if (sb.isEmpty()) {
      return new ToolCallResult<>(decoded.getResult(), true, null);
    }
    return new ToolCallResult<>(null, false, sb.toString());
  }
}
