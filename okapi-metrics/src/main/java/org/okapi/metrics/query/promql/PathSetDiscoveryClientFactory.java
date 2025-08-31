package org.okapi.metrics.query.promql;

import lombok.AllArgsConstructor;
import org.okapi.metrics.paths.PathSet;
import org.okapi.promql.eval.ts.SeriesDiscovery;

@AllArgsConstructor
public class PathSetDiscoveryClientFactory implements SeriesDiscoveryFactory{
    PathSet pathSet;
    @Override
    public SeriesDiscovery get(String tenantId) {
        return new PathSetSeriesDiscovery(tenantId, pathSet);
    }
}
