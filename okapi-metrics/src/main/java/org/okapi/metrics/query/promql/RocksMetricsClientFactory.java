package org.okapi.metrics.query.promql;

import java.util.Optional;
import org.okapi.Statistics;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.stats.StatisticsRestorer;
import org.okapi.promql.eval.ts.TimeseriesClient;

public class RocksMetricsClientFactory implements TimeSeriesClientFactory {
  ShardsAndSeriesAssigner shardsAndSeriesAssigner;
  PathRegistry pathRegistry;
  RocksStore rocksStore;
  StatisticsRestorer<Statistics> unmarshaller;

  public RocksMetricsClientFactory(PathRegistry pathRegistry, RocksStore rocksStore, StatisticsRestorer<Statistics> unmarshaller) {
    this.pathRegistry = pathRegistry;
    this.rocksStore = rocksStore;
    this.unmarshaller = unmarshaller;
  }

  public void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner) {
    this.shardsAndSeriesAssigner = shardsAndSeriesAssigner;
  }

  @Override
  public Optional<TimeseriesClient> getClient(String tenantId) {
    if (shardsAndSeriesAssigner == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        new RocksMetricsClient(
            shardsAndSeriesAssigner, pathRegistry, rocksStore, unmarshaller, tenantId));
  }
}
