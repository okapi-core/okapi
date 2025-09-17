package org.okapi.metrics.query.promql;

import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.promql.eval.ts.TimeseriesClient;

import java.util.Optional;

public interface TsClientFactory {
    Optional<TimeseriesClient> getClient(String tenantId);
    void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner);
}
