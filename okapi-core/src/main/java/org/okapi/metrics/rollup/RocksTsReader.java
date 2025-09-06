package org.okapi.metrics.rollup;

import static org.okapi.metrics.rollup.HashFns.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.Statistics;
import org.okapi.exceptions.ExceptionUtils;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.rocks.RocksDbReader;
import org.okapi.metrics.stats.StatisticsRestorer;
import org.rocksdb.RocksDBException;

@Slf4j
@AllArgsConstructor
public class RocksTsReader implements TsReader {
  RocksDbReader rocksDB;
  StatisticsRestorer<Statistics> unMarshaller;

  protected static long unQuantize(long ts, RES_TYPE resType) {
    return switch (resType) {
      case SECONDLY -> ts * 1000;
      case MINUTELY -> ts * 1000 * 60;
      case HOURLY -> ts * 1000 * 3600;
    };
  }

  private Optional<Statistics> readBucket(String bucket) {
    try {
      var stats = rocksDB.get(bucket.getBytes());
      if (stats == null) {
        return Optional.empty();
      } else {
        return Optional.of(unMarshaller.deserialize(stats));
      }
    } catch (RocksDBException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<Statistics> secondlyStats(String series, long when) {
    return readBucket(HashFns.secondlyBucket(series, when));
  }

  @Override
  public Optional<Statistics> minutelyStats(String series, long when) {
    return readBucket(HashFns.minutelyBucket(series, when));
  }

  @Override
  public Optional<Statistics> hourlyStats(String series, long when) {
    return readBucket(HashFns.hourlyBucket(series, when));
  }

  @Override
  public Optional<Statistics> getStat(String key) {
    return readBucket(key);
  }

  @Override
  public ScanResult scan(
      String timeSeries, long startTs, long endTs, AGG_TYPE agg, RES_TYPE resType) {
    final var timestamps = new ArrayList<Long>();
    final var values = new ArrayList<Float>();

    final long startQ = quantize(startTs, resType);
    final long endQ = quantize(endTs, resType);

    for (long q = startQ; q <= endQ; q++) {
      final long unqTs = unQuantize(q, resType);
      final var key = keyFn(timeSeries, unqTs, resType);
      try {
        final var bytes = rocksDB.get(key.getBytes());
        if (bytes == null) continue;
        final var stats = unMarshaller.deserialize(bytes);
        timestamps.add(unqTs);
        values.add(aggregate(stats, agg));
      } catch (RocksDBException e) {
        log.error("Could not read key from RocksDB due to {}", ExceptionUtils.debugFriendlyMsg(e));
      }
    }
    return new ScanResult(timeSeries, timestamps, values);
  }

  @Override
  public Map<Long, Statistics> scan(String series, long from, long to, RES_TYPE resolution) {
    Map<Long, Statistics> readings = new HashMap<>();
    final long startQ = quantize(from, resolution);
    final long endQ = quantize(to, resolution);
    for (long q = startQ; q <= endQ; q++) {
      final long unqTs = unQuantize(q, resolution);
      final var key = keyFn(series, unqTs, resolution);
      try {
        final var bytes = rocksDB.get(key.getBytes());
        if (bytes == null) continue;
        final var stats = unMarshaller.deserialize(bytes);
        readings.put(unqTs, stats);
      } catch (RocksDBException e) {
        log.error("Could not read key from RocksDB due to {}", ExceptionUtils.debugFriendlyMsg(e));
      }
    }
    return readings;
  }

  public static String keyFn(String timeSeries, long t, RES_TYPE resType) {
    return switch (resType) {
      case SECONDLY -> secondlyBucket(timeSeries, t);
      case MINUTELY -> minutelyBucket(timeSeries, t);
      case HOURLY -> hourlyBucket(timeSeries, t);
    };
  }

  protected static long quantize(long ts, RES_TYPE resType) {
    return switch (resType) {
      case SECONDLY -> ts / 1000;
      case MINUTELY -> ts / 1000 / 60;
      case HOURLY -> ts / 1000 / 3600;
    };
  }

  protected float aggregate(Statistics stat, AGG_TYPE agg) {
    return switch (agg) {
      case AVG -> stat.avg();
      case COUNT -> stat.getCount();
      case SUM -> stat.getSum();
      case MIN -> stat.min();
      case MAX -> stat.max();
      case P50 -> stat.percentile(0.50);
      case P75 -> stat.percentile(0.75);
      case P90 -> stat.percentile(0.90);
      case P95 -> stat.percentile(0.95);
      case P99 -> stat.percentile(0.99);
    };
  }
}
