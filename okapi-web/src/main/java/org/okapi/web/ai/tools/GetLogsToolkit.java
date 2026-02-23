package org.okapi.web.ai.tools;

import org.okapi.web.ai.tools.params.LogQuery;
import org.okapi.web.ai.tools.signals.LogsSignal;

public interface GetLogsToolkit extends AiToolkit {
  ToolCallResult<LogsSignal> getLogs(QueryContext context, LogQuery seriesQuery);
}
