package org.okapi.metrics.stats;

import org.okapi.metrics.common.MetricsContext;

public class NoOpOutlierListener implements OutlierListener {
  @Override
  public void notify(MetricsContext metricsContext, float f) {}
}
