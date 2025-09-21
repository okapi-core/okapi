package org.okapi.metrics.cas;

import com.google.api.client.util.Lists;
import com.google.common.primitives.Ints;
import java.time.Duration;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.Statistics;
import org.okapi.metrics.Merger;
import org.okapi.metrics.cas.dao.SketchesDao;
import org.okapi.metrics.cas.dto.*;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.MetricsPathParser;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.pojos.SUM_TYPE;
import org.okapi.metrics.pojos.results.GaugeScan;
import org.okapi.metrics.pojos.results.HistoScan;
import org.okapi.metrics.pojos.results.SumScan;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.metrics.stats.HistoStats;
import org.okapi.metrics.stats.StatisticsRestorer;
import org.okapi.metrics.stats.UpdatableStatistics;

@Slf4j
@AllArgsConstructor
public class CasTsReader implements TsReader {
  public static final Long SEC = Duration.ofSeconds(1).toMillis();
  public static final Long MIN = Duration.ofMinutes(1).toMillis();
  public static final Long HR = Duration.ofHours(1).toMillis();

  SketchesDao sketchesDao;
  Supplier<UpdatableStatistics> newStatSupplier;
  StatisticsRestorer<UpdatableStatistics> unmarshaller;
  Merger<UpdatableStatistics> merger;

  @Override
  public GaugeScan scanGauge(
      String series, long from, long to, AGG_TYPE aggregation, RES_TYPE resolution) {
    var reading = scanGauge(series, from, to, resolution);
    var reduced = new TreeMap<Long, Float>();
    for (var entry : reading.keySet()) {
      var agg = aggregate(reading.get(entry), aggregation);
      reduced.put(entry, agg);
    }
    var ts = new ArrayList<>(reduced.keySet());
    var vals = ts.stream().map(reduced::get).toList();
    return new GaugeScan(series, ts, vals);
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

  @Override
  public HistoScan scanHisto(String series, long from, long to) {
    var ubCount = new TreeMap<Float, Integer>();
    var infCount = 0;
    var parsed = MetricsPathParser.parse(series);
    var tenant = parsed.get().tenantId();
    var path = MetricPaths.localPath(parsed.get().name(), parsed.get().tags());
    var st = from / SEC;
    var en = to / SEC;
    for (HistoSketch histo : sketchesDao.scanHistoSketches(tenant, path, st, en)) {
      if (histo.getEndSecond() > en) continue;
      var sketch = HistoStats.deserialize(histo.getSketch().array());
      var buckets = sketch.getBuckets();
      var counts = sketch.getBucketCounts();
      for (int i = 0; i < buckets.length; i++) {
        var bucket = buckets[i];
        var count = counts[i];
        var updated = count + ubCount.getOrDefault(bucket, 0);
        ubCount.put(bucket, updated);
      }
      infCount += counts[buckets.length];
    }
    var bounds = Lists.newArrayList(ubCount.keySet());
    var counts = new ArrayList<>(ubCount.values());
    counts.add(infCount);
    return new HistoScan(series, from, to, bounds, counts);
  }

  @Override
  public SumScan scanSum(String series, long from, long to, long windowSize, SUM_TYPE sumType) {
    var blockStart = from / SEC;
    var blockEnd = to / SEC;
    var expectedDiff = windowSize / SEC;
    var parsed = MetricsPathParser.parse(series);
    var tenant = parsed.get().tenantId();
    var path = MetricPaths.localPath(parsed.get().name(), parsed.get().tags());
    var sketches = sketchesDao.scanCountSketches(tenant, path, blockStart, blockEnd);
    var reading = new TreeMap<Long, Integer>();
    for (var sketch : sketches) {
      var diff = sketch.getEndSecond() - sketch.getStartSecond();
      if (diff > expectedDiff) continue;
      var val = Ints.fromByteArray(sketch.getSketch().array());
      var time = sketch.getStartSecond() * SEC;
      reading.put(time, val);
    }
    return new SumScan(
        tenant,
        Lists.newArrayList(reading.keySet()),
        windowSize,
        Lists.newArrayList(reading.values()));
  }

  @Override
  public Map<Long, ? extends Statistics> scanGauge(
      String series, long from, long to, RES_TYPE resolution) {
    return switch (resolution) {
      case SECONDLY -> scanGauageSecondlyRes(series, from, to);
      case MINUTELY -> scanGauageMinutelyRes(series, from, to);
      case HOURLY -> scanGauageHourlyRes(series, from, to);
    };
  }

  public Map<Long, UpdatableStatistics> scanGauageSecondlyRes(String series, long from, long to) {
    var st = from / SEC;
    var en = to / SEC;
    var parsed = MetricsPathParser.parse(series);
    var tenant = parsed.get().tenantId();
    var path = MetricPaths.localPath(parsed.get().name(), parsed.get().tags());
    var range = sketchesDao.scanSecondly(tenant, path, st, en);
    var grouped =
        merge(range, (sk) -> ((GaugeSketchSecondly) sk).getSecondBlock() * SEC, (l, r) -> r);

    return grouped;
  }

  public Map<Long, UpdatableStatistics> scanGauageMinutelyRes(String series, long from, long to) {
    var st = from / MIN;
    var en = to / MIN;
    var parsed = MetricsPathParser.parse(series);
    var tenant = parsed.get().tenantId();
    var path = MetricPaths.localPath(parsed.get().name(), parsed.get().tags());
    var range = sketchesDao.scanMinutely(tenant, path, st, en);
    var grouped =
        merge(
            range,
            (sk) -> ((GaugeSketchMinutely) sk).getMinuteBlock() * MIN,
            (l, r) -> merger.merge(l, r));

    return grouped;
  }

  public Map<Long, UpdatableStatistics> scanGauageHourlyRes(String series, long from, long to) {
    var st = from / HR;
    var en = to / HR;
    var parsed = MetricsPathParser.parse(series);
    var tenant = parsed.get().tenantId();
    var path = MetricPaths.localPath(parsed.get().name(), parsed.get().tags());
    var range = sketchesDao.scanHourly(tenant, path, st, en);
    var grouped =
        merge(
            range,
            (sk) -> ((GaugeSketchHourly) sk).getHrBlock() * HR,
            (l, r) -> merger.merge(l, r));

    return grouped;
  }

  public Map<Long, UpdatableStatistics> merge(
      Iterable<? extends Sketchable> sketches,
      Function<Sketchable, Long> getTsFn,
      BiFunction<UpdatableStatistics, UpdatableStatistics, UpdatableStatistics> merger) {
    var map = new TreeMap<Long, UpdatableStatistics>();
    for (var sketch : sketches) {
      var time = getTsFn.apply(sketch);
      var cur = map.getOrDefault(time, newStatSupplier.get());
      var val = unmarshaller.deserialize(sketch.getSketch().array());
      var merged = merger.apply(cur, val);
      map.put(getTsFn.apply(sketch), merged);
    }
    return map;
  }

  @Override
  public Optional<Statistics> secondlyStats(String series, long when) {
    var parsed = MetricsPathParser.parse(series);
    var tenant = parsed.get().tenantId();
    var path = MetricPaths.localPath(parsed.get().name(), parsed.get().tags());
    var block = when / SEC;
    var gauge = sketchesDao.getSecondlySketch(tenant, path, block);
    return Optional.ofNullable(unmarshaller.deserialize(gauge.getSketch().array()));
  }

  @Override
  public Optional<Statistics> minutelyStats(String series, long when) {
    var parsed = MetricsPathParser.parse(series);
    var tenant = parsed.get().tenantId();
    var path = MetricPaths.localPath(parsed.get().name(), parsed.get().tags());
    var block = when / MIN;
    var gauge = sketchesDao.getMinutelySketch(tenant, path, block);
    return Optional.ofNullable(unmarshaller.deserialize(gauge.getSketch().array()));
  }

  @Override
  public Optional<Statistics> hourlyStats(String series, long when) {
    var parsed = MetricsPathParser.parse(series);
    var tenant = parsed.get().tenantId();
    var path = MetricPaths.localPath(parsed.get().name(), parsed.get().tags());
    var block = when / HR;
    var gauge = sketchesDao.getHourlyBlock(tenant, path, block);
    return Optional.ofNullable(unmarshaller.deserialize(gauge.getSketch().array()));
  }

  @Override
  public Optional<Statistics> getStat(String key) {
    throw new UnsupportedOperationException(
        "This operation is not supported for this implementation.");
  }
}
