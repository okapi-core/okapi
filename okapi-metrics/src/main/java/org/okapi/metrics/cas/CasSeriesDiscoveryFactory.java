package org.okapi.metrics.cas;

import org.okapi.metrics.query.promql.SeriesDiscoveryFactory;
import org.okapi.promql.eval.ts.SeriesDiscovery;

public class CasSeriesDiscoveryFactory implements SeriesDiscoveryFactory{
    @Override
    public SeriesDiscovery get(String tenantId) {
        return null;
    }
}
