package org.okapi.web.ai.tools.backends.datadog;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.okapi.agent.dto.JOB_STATUS;
import org.okapi.agent.dto.PendingJob;
import org.okapi.web.ai.tools.GetLogsToolkit;
import org.okapi.web.ai.tools.QueryContext;
import org.okapi.web.ai.tools.ToolCallResult;
import org.okapi.web.ai.tools.params.LogQuery;
import org.okapi.web.ai.tools.signals.LogsSignal;
import org.okapi.web.service.federation.dispatcher.JobDispatcher;

public class DataDogGetLogsToolkit implements GetLogsToolkit {

  JobDispatcher dispatcher;
  DatadogQueryWriter datadogQueryWriter;
  ToolConfig toolConfig;

  public DataDogGetLogsToolkit(
      JobDispatcher dispatcher, DatadogQueryWriter datadogQueryWriter, ToolConfig toolConfig) {
    this.dispatcher = dispatcher;
    this.datadogQueryWriter = datadogQueryWriter;
    this.toolConfig = toolConfig;
  }

  @Override
  public ToolCallResult<LogsSignal> getLogs(QueryContext context, LogQuery seriesQuery) {
    try {
      var queryId = UUID.randomUUID().toString();
      var ddgQuery = datadogQueryWriter.writeGetLogQuery(seriesQuery);
      var pendingJob =
          PendingJob.builder()
              .jobId(queryId)
              .jobStatus(JOB_STATUS.PENDING)
              .sourceId(context.sourceId())
              .spec(ddgQuery)
              .build();
      var future = dispatcher.dispatchJob(context.getSession().orgId(), pendingJob);
      var result = future.get(toolConfig.timeoutMillis(), TimeUnit.MILLISECONDS);
      if (result.error() != null) {
        return new ToolCallResult<>(null, false, result.error());
      }

      var decoded = DatadogLogsDecoder.mapJsonRecord(result.data());
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
    } catch (Exception e) {
      return new ToolCallResult<>(null, false, e.getMessage());
    }
  }
}
