package org.okapi.metrics.rollup;

import static com.google.api.client.util.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import org.okapi.Statistics;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.stats.StatisticsRestorer;

public class RocksReaderSupplier implements Function<Integer, Optional<RocksTsReader>> {

  PathRegistry pathRegistry;
  StatisticsRestorer<Statistics> unMarshaller;
  RocksStore rocksStore;

  public RocksReaderSupplier(
      PathRegistry pathRegistry,
      StatisticsRestorer<Statistics> restorer,
      RocksStore rocksStore) {
    this.pathRegistry = checkNotNull(pathRegistry);
    this.unMarshaller = checkNotNull(restorer);
    this.rocksStore = rocksStore;
  }

  @Override
  public Optional<RocksTsReader> apply(Integer shard) {
    var path = pathRegistry.rocksPath(shard);
    try {
      var optDb = this.rocksStore.rocksReader(path);
      return optDb.map(rocksDB -> new RocksTsReader(rocksDB, unMarshaller));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
