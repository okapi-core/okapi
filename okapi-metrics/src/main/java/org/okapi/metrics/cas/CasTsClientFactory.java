package org.okapi.metrics.cas;

import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.query.promql.TsClientFactory;
import org.okapi.promql.eval.ts.TimeseriesClient;

import java.util.Optional;

public class CasTsClientFactory implements TsClientFactory {
    @Override
    public Optional<TimeseriesClient> getClient(String tenantId) {
        return Optional.empty();
    }

    @Override
    public void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner) {

    }
}
