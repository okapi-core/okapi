package org.okapi.traces.page;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogAndDropWriteFailedListener implements WriteFailedListener {
  private static final Logger LOG = Logger.getLogger(LogAndDropWriteFailedListener.class.getName());

  @Override
  public void onWriteFaile(String tenantId, String application, SpanPage page) {
    LOG.log(
        Level.WARNING,
        () ->
            "Trace file write failed for tenant="
                + tenantId
                + ", app="
                + application
                + ", window="
                + page.getTsStartMillis()
                + "-"
                + page.getTsEndMillis()
                + ": dropped");
  }
}
