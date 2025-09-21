package org.okapi.metrics.cas;

import lombok.AllArgsConstructor;
import org.okapi.metrics.query.promql.SeriesDiscoveryFactory;
import org.okapi.metrics.rollup.TsSearcher;
import org.okapi.promql.eval.ts.SeriesDiscovery;

@AllArgsConstructor
public class CasSeriesDiscoveryFactory implements SeriesDiscoveryFactory {
  private final TsSearcher searcher;

  @Override
  public SeriesDiscovery get(String tenantId) {
    return new CasSeriesDiscovery(tenantId, searcher);
  }
}
