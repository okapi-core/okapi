package org.okapi.metrics.fdb;

import com.apple.foundationdb.Database;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.Statistics;
import org.okapi.metrics.Merger;
import org.okapi.metrics.fdb.tuples.BUCKET_TYPE;
import org.okapi.metrics.fdb.tuples.MetricWriteTuple;
import org.okapi.metrics.fdb.tuples.PointTuple;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.rollup.ScanResult;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.metrics.stats.StatisticsRestorer;
import org.okapi.metrics.stats.UpdatableStatistics;

@Slf4j
@AllArgsConstructor
public class FdbTsReader implements TsReader {

  Database database;
  Merger<UpdatableStatistics> merger;
  StatisticsRestorer<UpdatableStatistics> unmarshaller;

  @Override
  public ScanResult scan(
      String series, long from, long to, AGG_TYPE aggregation, RES_TYPE resolution) {
    // add the reduction
    var reading = scan(series, from, to, resolution);
    var reduced = new TreeMap<Long, Float>();
    for (var entry : reading.keySet()) {
      var agg = aggregate(reading.get(entry), aggregation);
      reduced.put(entry, agg);
    }
    var ts = new ArrayList<>(reduced.keySet());
    var vals = ts.stream().map(reduced::get).toList();
    return new ScanResult(series, ts, vals);
  }

  public Float aggregate(Statistics statistics, AGG_TYPE aggType) {
    return switch (aggType) {
      case SUM -> statistics.getSum();
      case COUNT -> statistics.getCount();
      case AVG -> statistics.avg();
      case MIN -> statistics.percentile(0.0);
      case MAX -> statistics.percentile(1.0);
      case P50 -> statistics.percentile(0.5);
      case P75 -> statistics.percentile(0.75);
      case P90 -> statistics.percentile(0.9);
      case P95 -> statistics.percentile(0.95);
      case P99 -> statistics.percentile(0.99);
    };
  }

  public Optional<UpdatableStatistics> merge(Collection<UpdatableStatistics> toMerge) {
    UpdatableStatistics merged = null;
    for (var stat : toMerge) {
      if (merged == null) merged = stat;
      else {
        merged = merger.merge(merged, stat);
      }
    }
    return Optional.ofNullable(merged);
  }

  @Override
  public Map<Long, Statistics> scan(String series, long from, long to, RES_TYPE resolution) {
    BUCKET_TYPE bucketType =
        switch (resolution) {
          case HOURLY -> BUCKET_TYPE.H;
          case MINUTELY -> BUCKET_TYPE.M;
          case SECONDLY -> BUCKET_TYPE.S;
        };

    var quantizer =
        switch (resolution) {
          case SECONDLY -> 1000;
          case MINUTELY -> 60_000;
          case HOURLY -> 3600_000;
        };

    var blockStart = from / quantizer;
    var blockEnd = to / quantizer;
    var start = new PointTuple(series, blockStart, bucketType);
    var range = start.inclusiveRange(blockEnd);
    // common operation -> scan range -> get stats -> group by and mere
    var reading = readRange(range[0], range[1], quantizer);
    log.info("Got {} readings for resolution: {}", reading.size(), resolution);
    return merge(reading);
  }

  public Multimap<Long, UpdatableStatistics> readRange(
      byte[] rangeStart, byte[] rangeEnd, long timeScale) {
    var reading = HashMultimap.<Long, UpdatableStatistics>create();
    database.run(
        tr -> {
          var iterable = tr.getRange(rangeStart, rangeEnd).iterator();
          while (iterable.hasNext()) {
            var kv = iterable.next();
            var metric = MetricWriteTuple.fromKey(kv.getKey());
            var t = timeScale * metric.getBucket();
            var v = unmarshaller.deserialize(kv.getValue());
            reading.put(t, v);
          }
          return null;
        });
    return reading;
  }

  public Map<Long, Statistics> merge(Multimap<Long, UpdatableStatistics> reading) {
    var reduced = new TreeMap<Long, Statistics>();
    for (var entry : reading.keySet()) {
      var merged = merge(reading.get(entry));
      if (merged.isEmpty()) continue;
      reduced.put(entry, merged.get());
    }
    return reduced;
  }

  @Override
  public Optional<Statistics> secondlyStats(String series, long when) {
    return getReadingAt(series, when, RES_TYPE.SECONDLY);
  }

  public Optional<Statistics> getReadingAt(String series, long when, RES_TYPE resType) {
    var scale = scale(resType);
    var bucket = when / scale;
    var bucketType =
        switch (resType) {
          case SECONDLY -> BUCKET_TYPE.S;
          case MINUTELY -> BUCKET_TYPE.M;
          case HOURLY -> BUCKET_TYPE.H;
        };
    var pointTuple = new PointTuple(series, bucket, bucketType);
    var range = pointTuple.pointQuery();
    var reading = readRange(range[0], range[1], scale);
    var merged = merge(reading);
    var time = scale * bucket;
    return Optional.ofNullable(merged.get(time));
  }

  public Long scale(RES_TYPE resType) {
    return switch (resType) {
      case HOURLY -> 3600_000L;
      case MINUTELY -> 60_00L;
      case SECONDLY -> 1000L;
    };
  }

  @Override
  public Optional<Statistics> minutelyStats(String series, long when) {
    return getReadingAt(series, when, RES_TYPE.MINUTELY);
  }

  @Override
  public Optional<Statistics> hourlyStats(String series, long when) {
    return getReadingAt(series, when, RES_TYPE.HOURLY);
  }

  @Override
  public Optional<Statistics> getStat(String key) {
    throw new IllegalArgumentException("This does not apply here.");
  }
}
