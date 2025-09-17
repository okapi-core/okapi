package org.okapi.metrics.fdb;

import lombok.AllArgsConstructor;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.query.promql.TsClientFactory;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.promql.eval.ts.TimeseriesClient;

import java.util.Optional;

@AllArgsConstructor
public class FdbTsClientFactory implements TsClientFactory {

  TsReader tsReader;

  @Override
  public Optional<TimeseriesClient> getClient(String tenantId) {
    return Optional.of(new FdbTsClient(tenantId, tsReader));
  }

  @Override
  public void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner) {}
}
