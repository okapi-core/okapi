package org.okapi.metrics.query.promql;

import org.okapi.promql.eval.ts.TimeseriesClient;

public interface TimeSeriesClientFactory {
    TimeseriesClient getClient(String tenantId);
}
