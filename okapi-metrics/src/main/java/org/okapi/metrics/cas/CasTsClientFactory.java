package org.okapi.metrics.cas;

import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.query.promql.TsClientFactory;
import org.okapi.promql.eval.ts.TsClient;

import java.util.Optional;

public class CasTsClientFactory implements TsClientFactory {
    @Override
    public Optional<TsClient> getClient(String tenantId) {
        return Optional.empty();
    }

    @Override
    public void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner) {

    }
}
