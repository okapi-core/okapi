package org.okapi.web.investigation.ctx.finders;

import java.util.List;

public interface MetricPathFinder {
  public List<MetricPath> findRelateMetrics(String dependencyName);
}
