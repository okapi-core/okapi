package org.okapi.metrics.rollup;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.datasketches.kll.KllFloatsSketch;
import org.okapi.clock.Clock;
import org.okapi.clock.SystemClock;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.stats.KllSketchRestorer;
import org.okapi.metrics.stats.Statistics;

public class RollupSeries {

  public static final String MAGIC_NUMBER = "RollupSeriesStart";
  public static final String MAGIC_NUMBER_END = "RollupSeriesEnd";
  public static final Long ADMISSION_WINDOW = Duration.of(24, ChronoUnit.HOURS).toMillis();

  private final Map<String, Statistics> stats;
  private Clock clock;

  public RollupSeries() {
    this.stats = new ConcurrentHashMap<>(20_000);
    this.clock = new SystemClock();
  }

  public RollupSeries(Clock clock) {
    this();
    this.clock = clock;
  }

  protected Optional<Statistics> getStats(String k) {
    return Optional.ofNullable(stats.get(k));
  }

  protected Set<String> keys() {
    return Collections.unmodifiableSet(stats.keySet()); // live, weakly-consistent
  }

  protected void put(String k, Statistics s) {
    stats.put(k, s);
  }

  // -------------------- Write path --------------------

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

    // todo: for now we're using heap memory to store KLLStats which adds to memory pressure, this should be replaced with off-heap memory instead
    final var sec =
        stats.computeIfAbsent(secKey, k -> new Statistics(KllFloatsSketch.newHeapInstance(200)));
    final var min =
        stats.computeIfAbsent(minKey, k -> new Statistics(KllFloatsSketch.newHeapInstance(200)));
    final var hr =
        stats.computeIfAbsent(hrKey, k -> new Statistics(KllFloatsSketch.newHeapInstance(200)));

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
    final var snapshot = new ArrayList<Map.Entry<String, Statistics>>(stats.size());
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
      var st = Statistics.deserialize(OkapiIo.readBytes(is), new KllSketchRestorer());
      stats.put(k, st);
    }
    OkapiIo.checkMagicNumber(is, MAGIC_NUMBER_END);
  }

  public static RollupSeries restore(Path checkpointPath)
      throws IOException, StreamReadingException {
    try (var fis = new FileInputStream(checkpointPath.toFile())) {
      return restore(fis);
    }
  }

  public static RollupSeries restore(FileInputStream fis)
      throws IOException, StreamReadingException {
    var series = new RollupSeries();
    series.loadCheckpoint(fis);
    return series;
  }

  public static RollupSeries restore(DataInputStream dis)
      throws IOException, StreamReadingException {
    var series = new RollupSeries();
    series.loadCheckpoint(dis);
    return series;
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

  protected Statistics getSecondlyStatistics(String timeSeries, long ts) {
    return stats.get(secondlyShard(timeSeries, ts));
  }

  protected Statistics getMinutelyStatistics(String timeSeries, long ts) {
    return stats.get(minutelyShard(timeSeries, ts));
  }

  protected Statistics getHourlyStatistics(String timeSeries, long ts) {
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
