package org.okapi.metrics.rollup;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.stats.KllSketchRestorer;
import org.okapi.metrics.stats.Statistics;
import org.okapi.metrics.stats.StatisticsRestorer;

public class RollupSeries<T extends Statistics> {

  // todo: KVCache as a simple way to get TP out.
  // todo: refactor the code, run integ test, check everything is fine.
  // todo: integrate LMDB, RocksDB page swapper
  // todo: add fixed memory buffer
  // todo: add "forward-while-resharding" logic as a stretch goal
  // todo: de-couple restoration from initiation -> also use checksums to avoid reading corrupted shit.
  // todo: add write TPS per tenant, cardinality limits are implied
  // todo: KllSketch lightweight estimator -> add a linear formula to do this quickly
  // todo: refactor magic number end checks or add CRC32 checksums to deal with snapshots
  public static final String MAGIC_NUMBER = "RollupSeriesStart";
  public static final String MAGIC_NUMBER_END = "RollupSeriesEnd";
  public static final Long ADMISSION_WINDOW = Duration.of(24, ChronoUnit.HOURS).toMillis();

  private final Map<String, T> stats;
  private final StatisticsRestorer<T> restorer;
  private final Supplier<T> newStatsSupplier;

  public RollupSeries(StatisticsRestorer<T> restorer, Supplier<T> newStatsSupplier) {
    this.stats = new ConcurrentHashMap<>(2_000);
    this.restorer = restorer;
    this.newStatsSupplier = newStatsSupplier;
  }

  protected Optional<T> getStats(String k) {
    return Optional.ofNullable(stats.get(k));
  }

  protected Set<String> keys() {
    return Collections.unmodifiableSet(stats.keySet()); // live, weakly-consistent
  }

  protected void put(String k, T s) {
    stats.put(k, s);
  }

  public void writeBatch(MetricsContext ctx, String timeSeries, long[] ts, float[] vals)
      throws OutsideWindowException {
    for (int i = 0; i < ts.length; i++) {
      write(ctx, timeSeries, ts[i], vals[i]);
    }
  }

  public void write(MetricsContext ctx, String timeSeries, long ts, float val) {
    final var secKey = secondlyShard(timeSeries, ts);
    final var minKey = minutelyShard(timeSeries, ts);
    final var hrKey = hourlyShard(timeSeries, ts);

    final var sec =
        stats.computeIfAbsent(secKey, k -> newStatsSupplier.get());
    final var min =
        stats.computeIfAbsent(minKey, k -> newStatsSupplier.get());
    final var hr =
        stats.computeIfAbsent(hrKey, k -> newStatsSupplier.get());

    sec.update(ctx, val);
    min.update(ctx, val);
    hr.update(ctx, val);
  }

  // -------------------- Read / aggregation --------------------

  public ScanResult scan(
      String timeSeries, long startTs, long endTs, AGG_TYPE agg, RES_TYPE resType) {
    final var timestamps = new ArrayList<Long>();
    final var values = new ArrayList<Float>();

    final long startQ = quantize(startTs, resType);
    final long endQ = quantize(endTs, resType);

    for (long q = startQ; q <= endQ; q++) {
      final long unqTs = unQuantize(q, resType);
      final var key = shard(timeSeries, unqTs, resType);
      final var st = stats.get(key);
      if (st == null) continue;

      timestamps.add(unqTs);
      values.add(aggregate(st, agg));
    }
    return new ScanResult(timeSeries, timestamps, values);
  }

  public float count(String timeSeries, long startTs, long endTs, RES_TYPE resType) {
    float c = 0f;
    final long startQ = quantize(startTs, resType);
    final long endQ = quantize(endTs, resType);
    for (long q = startQ; q <= endQ; q++) {
      final long unqTs = unQuantize(q, resType);
      final var key = shard(timeSeries, unqTs, resType); // use :s/:m/:h
      final var st = stats.get(key);
      if (st != null) c += st.getCount();
    }
    return c;
  }

  protected float aggregate(T stat, AGG_TYPE agg) {
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

  protected static long quantize(long ts, RES_TYPE resType) {
    return switch (resType) {
      case SECONDLY -> ts / 1000;
      case MINUTELY -> ts / 1000 / 60;
      case HOURLY -> ts / 1000 / 3600;
      case DAILY -> ts / 1000 / 3600 / 24;
    };
  }

  protected static long unQuantize(long ts, RES_TYPE resType) {
    return switch (resType) {
      case SECONDLY -> ts * 1000;
      case MINUTELY -> ts * 1000 * 60;
      case HOURLY -> ts * 1000 * 3600;
      case DAILY -> ts * 1000 * 3600 * 24;
    };
  }

  // -------------------- Checkpoint / restore --------------------

  public void checkpoint(Path checkpointPath) throws IOException {
    try (var fos = new FileOutputStream(checkpointPath.toFile())) {
      checkpoint(fos);
    }
  }

  public void checkpoint(OutputStream os) throws IOException {
    // Snapshot entries to keep count consistent with what we write
    final var snapshot = new ArrayList<Map.Entry<String, T>>(stats.size());
    stats.forEach(
        (k, v) -> {
          if (v != null) snapshot.add(Map.entry(k, v));
        });

    OkapiIo.writeString(os, MAGIC_NUMBER);
    OkapiIo.writeInt(os, snapshot.size());
    for (var e : snapshot) {
      OkapiIo.writeString(os, e.getKey());
      OkapiIo.writeBytes(os, e.getValue().serialize());
    }
    OkapiIo.writeString(os, MAGIC_NUMBER_END);
  }

  public void checkpoint(Set<String> keys, Path checkpointPath) throws IOException {
    try (var fos = new FileOutputStream(checkpointPath.toFile())) {
      checkpoint(keys, fos);
    }
  }

  public void checkpoint(Set<String> keys, OutputStream os) throws IOException {
    final var filtered =
        keys.stream()
            .map(k -> Map.entry(k, stats.get(k)))
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toList());

    OkapiIo.writeString(os, MAGIC_NUMBER);
    OkapiIo.writeInt(os, filtered.size());
    for (var e : filtered) {
      OkapiIo.writeString(os, e.getKey());
      OkapiIo.writeBytes(os, e.getValue().serialize());
    }
    OkapiIo.writeString(os, MAGIC_NUMBER_END);
  }

  public void loadCheckpoint(InputStream is) throws StreamReadingException, IOException {
    OkapiIo.checkMagicNumber(is, MAGIC_NUMBER);
    int n = OkapiIo.readInt(is);
    for (int i = 0; i < n; i++) {
      var k = OkapiIo.readString(is);
      var st = restorer.deserialize(OkapiIo.readBytes(is), new KllSketchRestorer());
      stats.put(k, st);
    }
    OkapiIo.checkMagicNumber(is, MAGIC_NUMBER_END);
  }


  // -------------------- Utilities --------------------

  public Set<String> listMetricPaths() {
    var out = new HashSet<String>();
    for (var k : stats.keySet()) {
      var split = k.split(":");
      if (split.length >= 2) {
        out.add(split[0] + ":" + split[1]);
      }
    }
    return out;
  }

  public void writeMetric(String metric, OutputStream os) throws IOException {
    var subset =
        stats.keySet().stream().filter(k -> k.startsWith(metric)).collect(Collectors.toSet());
    checkpoint(subset, os);
  }

  public static String hourlyShard(String timeSeries, long ts) {
    return timeSeries + ":h:" + (ts / 1000 / 3600);
  }

  public static String minutelyShard(String timeSeries, long ts) {
    return timeSeries + ":m:" + (ts / 1000 / 60);
  }

  public static String secondlyShard(String timeSeries, long ts) {
    return timeSeries + ":s:" + (ts / 1000);
  }

  public static String shard(String timeSeries, long t, RES_TYPE resType) {
    return switch (resType) {
      case SECONDLY -> secondlyShard(timeSeries, t);
      case MINUTELY -> minutelyShard(timeSeries, t);
      case HOURLY -> hourlyShard(timeSeries, t);
      case DAILY -> throw new IllegalArgumentException("DAILY not supported");
    };
  }

  protected T getSecondlyStatistics(String timeSeries, long ts) {
    return stats.get(secondlyShard(timeSeries, ts));
  }

  protected T getMinutelyStatistics(String timeSeries, long ts) {
    return stats.get(minutelyShard(timeSeries, ts));
  }

  protected T getHourlyStatistics(String timeSeries, long ts) {
    return stats.get(hourlyShard(timeSeries, ts));
  }

  public byte[] getSerializedStats(String key) {
    var st = stats.get(key);
    return (st == null) ? null : st.serialize();
  }

  public Set<String> getKeys() {
    return Collections.unmodifiableSet(stats.keySet());
  }
}
