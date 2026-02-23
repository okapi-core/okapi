/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.okapi.web.ai.tools.params.SpanQuery;
import org.okapi.web.ai.tools.signals.TracesSignal;

public interface GetTracesToolkit {
  ToolCallResult<TracesSignal> getSpans(QueryContext context, SpanQuery spanQuery)
      throws ExecutionException, InterruptedException, TimeoutException;
}
