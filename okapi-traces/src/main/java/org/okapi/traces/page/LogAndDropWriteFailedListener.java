package org.okapi.traces.page;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogAndDropWriteFailedListener implements WriteFailedListener {
  @Override
  public void onWriteFaile(String tenantId, String application, SpanPage page) {
    log.warn(
        "Trace file write failed for tenant={}, app={}, window={}-{}: dropped",
        tenantId,
        application,
        page.getTsStartMillis(),
        page.getTsEndMillis());
  }
}
