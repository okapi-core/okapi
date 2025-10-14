package org.okapi.metrics.query.promql;

import org.okapi.promql.eval.ts.SeriesDiscovery;

public interface SeriesDiscoveryFactory {
  SeriesDiscovery get(String tenantId);
}
