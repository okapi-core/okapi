package org.okapi.web.ai.tools;

import org.okapi.web.ai.tools.params.SpanQuery;
import org.okapi.web.ai.tools.signals.TracesSignal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface GetTracesToolkit {
  ToolCallResult<TracesSignal> getSpans(QueryContext context, SpanQuery spanQuery) throws ExecutionException, InterruptedException, TimeoutException;
}
