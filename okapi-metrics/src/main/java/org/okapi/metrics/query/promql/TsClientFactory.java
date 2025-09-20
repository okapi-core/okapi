package org.okapi.metrics.query.promql;

import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.promql.eval.ts.TsClient;

import java.util.Optional;

public interface TsClientFactory {
    Optional<TsClient> getClient(String tenantId);
    void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner);
}
