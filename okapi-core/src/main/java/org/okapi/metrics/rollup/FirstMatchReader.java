package org.okapi.metrics.rollup;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.okapi.Statistics;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.stats.StatisticsRestorer;

@AllArgsConstructor
public class FirstMatchReader implements TsReader {
  public List<TsReader> readers;

  @Override
  public ScanResult scan(
      String series, long from, long to, AGG_TYPE aggregation, RES_TYPE resolution) {
    for (var reader : readers) {
      var result = reader.scan(series, from, to, aggregation, resolution);
      if (!result.getTimestamps().isEmpty()) return result;
    }
    return ScanResult.builder()
        .timestamps(Collections.emptyList())
        .seriesName(series)
        .values(Collections.emptyList())
        .build();
  }

  @Override
  public Map<Long, Statistics> scan(
      String series, long from, long to, RES_TYPE resolution) {
    for (var reader : readers) {
      var result = reader.scan(series, from, to, resolution);
      if (!result.isEmpty()) return result;
    }
    return Collections.emptyMap();
  }

  @Override
  public int count(String series, long from, long to, RES_TYPE resolution) {
    for (var reader : readers) {
      var count = reader.count(series, from, to, resolution);
      if (count > 0) return count;
    }
    return 0;
  }

  @Override
  public Optional<Statistics> secondlyStats(String series, long when) {
    for (var reader : readers) {
      var stat = reader.secondlyStats(series, when);
      if (stat.isPresent()) return stat;
    }
    return Optional.empty();
  }

  @Override
  public Optional<Statistics> minutelyStats(String series, long when) {
    for (var reader : readers) {
      var stat = reader.minutelyStats(series, when);
      if (stat.isPresent()) return stat;
    }
    return Optional.empty();
  }

  @Override
  public Optional<Statistics> hourlyStats(String series, long when) {
    for (var reader : readers) {
      var stat = reader.hourlyStats(series, when);
      if (stat.isPresent()) return stat;
    }
    return Optional.empty();
  }

  @Override
  public Optional<Statistics> getStat(String key) {
    for (var reader : readers) {
      var stat = reader.getStat(key);
      if (stat.isPresent()) return stat;
    }
    return Optional.empty();
  }

  public static TsReader getFirstMatchReader(
      PathRegistry pathRegistry,
      RocksStore store,
      Supplier<StatisticsRestorer<Statistics>> unmarshallerFn,
      Collection<Integer> shards)
      throws IOException {
    var readers = new ArrayList<TsReader>();
    for (int shard : shards) {
      var dbPath = pathRegistry.rocksPath(shard);
      var reader = store.rocksReader(dbPath);
      if (reader.isEmpty()) continue;
      readers.add(new RocksTsReader(reader.get(), unmarshallerFn.get()));
    }
    return new FirstMatchReader(readers);
  }
}
