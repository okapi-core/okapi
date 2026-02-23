package org.okapi.promql.runtime;

import org.okapi.promql.eval.ts.SeriesDiscovery;

public interface SeriesDiscoveryFactory {
  SeriesDiscovery get(String tenantId);
}
