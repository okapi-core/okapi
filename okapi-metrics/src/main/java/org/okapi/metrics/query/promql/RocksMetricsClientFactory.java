package org.okapi.metrics.query.promql;

import lombok.AllArgsConstructor;
import org.okapi.Statistics;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.stats.StatisticsRestorer;
import org.okapi.promql.eval.ts.TimeseriesClient;

@AllArgsConstructor
public class RocksMetricsClientFactory implements TimeSeriesClientFactory {
  ShardsAndSeriesAssigner shardsAndSeriesAssigner;
  PathRegistry pathRegistry;
  RocksStore rocksStore;
  StatisticsRestorer<Statistics> unmarshaller;

  @Override
  public TimeseriesClient getClient(String tenantId) {
    return new RocksMetricsClient(
        shardsAndSeriesAssigner, pathRegistry, rocksStore, unmarshaller, tenantId);
  }
}
