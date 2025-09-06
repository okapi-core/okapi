package org.okapi.metrics.fdb;

import org.okapi.metrics.query.promql.SeriesDiscoveryFactory;
import org.okapi.promql.eval.ts.SeriesDiscovery;

public class FdbSeriesDiscoveryFactory implements SeriesDiscoveryFactory {
    @Override
    public SeriesDiscovery get(String tenantId) {
        return new FdbSeriesDiscovery();
    }
}
