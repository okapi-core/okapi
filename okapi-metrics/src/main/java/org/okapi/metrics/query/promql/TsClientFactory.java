package org.okapi.metrics.query.promql;

import java.util.Optional;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.promql.eval.ts.TsClient;

public interface TsClientFactory {
  Optional<TsClient> getClient(String tenantId);

  void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner);
}
