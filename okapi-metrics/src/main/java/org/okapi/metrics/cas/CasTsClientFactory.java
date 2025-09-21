package org.okapi.metrics.cas;

import java.util.Optional;
import org.okapi.metrics.cas.dao.TypeHintsDao;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.query.promql.TsClientFactory;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.promql.eval.ts.TsClient;

public class CasTsClientFactory implements TsClientFactory {

  private final TsReader reader;
  private final TypeHintsDao typeHintsDao;

  public CasTsClientFactory() {
    this.reader = null;
    this.typeHintsDao = null;
  }

  public CasTsClientFactory(TsReader reader, TypeHintsDao typeHintsDao) {
    this.reader = reader;
    this.typeHintsDao = typeHintsDao;
  }

  @Override
  public Optional<TsClient> getClient(String tenantId) {
    // If assignment is not yet provided, signal unavailability to callers.
    if (reader == null || typeHintsDao == null) return Optional.empty();

    var client = new CasTsClient();
    client.tenantId = tenantId;
    client.reader = reader;
    client.typeHintsDao = typeHintsDao;
    return Optional.of(client);
  }

  @Override
  public void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner) {
    return;
  }
}
