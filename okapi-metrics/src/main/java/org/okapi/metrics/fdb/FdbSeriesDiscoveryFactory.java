package org.okapi.metrics.fdb;

import lombok.AllArgsConstructor;
import org.okapi.metrics.query.promql.SeriesDiscoveryFactory;
import org.okapi.metrics.rollup.TsSearcher;
import org.okapi.promql.eval.ts.SeriesDiscovery;

@AllArgsConstructor
public class FdbSeriesDiscoveryFactory implements SeriesDiscoveryFactory {
  TsSearcher searcher;

  @Override
  public SeriesDiscovery get(String tenantId) {
    return new FdbSeriesDiscovery(tenantId, searcher);
  }
}
