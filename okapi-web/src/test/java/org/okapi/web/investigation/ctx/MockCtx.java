package org.okapi.web.investigation.ctx;

import org.okapi.web.ai.tools.signals.LogsSignal;
import org.okapi.web.ai.tools.signals.TracesSignal;

public class MockCtx {

  public static LogsSignal getLogsSignal() {
    return null;
  }

  public static TracesSignal getTracesSignal() {
    return null;
  }
}
