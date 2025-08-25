package org.okapi.metrics.rollup;

import static com.google.api.client.util.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import org.okapi.metrics.rocks.RocksPathSupplier;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.stats.Statistics;
import org.okapi.metrics.stats.StatisticsRestorer;

public class RocksReaderSupplier implements Function<Integer, Optional<RocksReader>> {

  RocksPathSupplier rocksPathSupplier;
  StatisticsRestorer<Statistics> unMarshaller;
  RocksStore rocksStore;

  public RocksReaderSupplier(
      RocksPathSupplier rocksPathSupplier,
      StatisticsRestorer<Statistics> unMarshaller,
      RocksStore rocksStore) {
    this.rocksPathSupplier = checkNotNull(rocksPathSupplier);
    this.unMarshaller = checkNotNull(unMarshaller);
    this.rocksStore = rocksStore;
  }

  @Override
  public Optional<RocksReader> apply(Integer shard) {
    var path = rocksPathSupplier.apply(shard);
    try {
      var optDb = this.rocksStore.rocksReader(path);
      return optDb.map(rocksDB -> new RocksReader(rocksDB, unMarshaller));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
