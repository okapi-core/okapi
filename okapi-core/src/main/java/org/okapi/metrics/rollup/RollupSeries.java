package org.okapi.metrics.rollup;

import static com.google.api.client.util.Preconditions.checkNotNull;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.okapi.clock.Clock;
import org.okapi.exceptions.ExceptionUtils;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.WriteBackRequest;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.constants.ReaderIds;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.stats.Statistics;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.metrics.stats.StatisticsRestorer;

@Slf4j
public class RollupSeries<T extends Statistics> {

  public static final String MAGIC_NUMBER = "RollupSeriesStart";
  public static final String MAGIC_NUMBER_END = "RollupSeriesEnd";
  public static final Long ADMISSION_WINDOW = Duration.of(24, ChronoUnit.HOURS).toMillis();

  private final Map<String, T> stats;
  private final StatisticsRestorer<T> restorer;
  private final Supplier<T> newStatsSupplier;
  private final Map<String, Long> createTime;
  int shard;
  ScheduledExecutorService scheduledExecutorService;
  SharedMessageBox<WriteBackRequest> messageBox;

  public RollupSeries(StatisticsRestorer<T> restorer, Supplier<T> newStatsSupplier, int shard) {
    this.stats = new ConcurrentHashMap<>(2_000);
    this.restorer = restorer;
    this.newStatsSupplier = newStatsSupplier;
    this.shard = shard;
    this.createTime = new ConcurrentHashMap<>();
  }

  public void startFreezing(
      SharedMessageBox<WriteBackRequest> messageBox,
      ScheduledExecutorService scheduler,
      WriteBackSettings writeBackSettings) {
    this.messageBox = checkNotNull(messageBox);
    this.scheduledExecutorService = checkNotNull(scheduler);
    checkNotNull(writeBackSettings);
    startFreezer(writeBackSettings.getClock(), writeBackSettings.getHotWindow());
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
      throws InterruptedException, StatisticsFrozenException {
    for (int i = 0; i < ts.length; i++) {
      write(ctx, timeSeries, ts[i], vals[i]);
    }
  }

  public void write(MetricsContext ctx, String timeSeries, long ts, float val)
      throws InterruptedException, StatisticsFrozenException {
    final var secKey = HashFns.secondlyBucket(timeSeries, ts);
    final var minKey = HashFns.minutelyBucket(timeSeries, ts);
    final var hrKey = HashFns.hourlyBucket(timeSeries, ts);

    retryUpdate(() -> stats.computeIfAbsent(secKey, this::makeNew), ctx, val);
    retryUpdate(() -> stats.computeIfAbsent(minKey, this::makeNew), ctx, val);
    retryUpdate(() -> stats.computeIfAbsent(hrKey, this::makeNew), ctx, val);
  }

  private void retryUpdate(
      Supplier<Statistics> statisticsSupplier, MetricsContext context, float val) {
    var retries = 3;
    for (int i = 0; i < retries; i++) {
      try {
        statisticsSupplier.get().update(context, val);
        break;
      } catch (StatisticsFrozenException e) {
        log.info("Frozen stats, retrying.");
      }
    }
  }

  public T makeNew(String k) {
    var stats = newStatsSupplier.get();
    createTime.put(k, System.currentTimeMillis());
    return stats;
  }

  public void startFreezer(Clock clock, Duration period) {
    scheduledExecutorService.scheduleAtFixedRate(
        () -> {
          try {
            freezeAndShip(clock, period);
          } catch (InterruptedException e) {
            log.error("Shipping stopped.");
          } catch (Exception t) {
            log.error("This execution got an exception {}", ExceptionUtils.debugFriendlyMsg(t));
          }
        },
        0,
        period.toMillis(),
        TimeUnit.MILLISECONDS);
  }

  public void freezeAndShip(Clock clock, Duration hotWindow) throws InterruptedException {
    var keys = createTime.keySet();
    for (var key : keys) {
      var hasExpired = clock.currentTimeMillis() - createTime.get(key) >= hotWindow.toMillis();
      if (hasExpired) {
        var stat = stats.get(key);
        stat.freeze();
        log.debug("Writing to box.");
        this.messageBox.push(
            new WriteBackRequest(MetricsContext.createContext("test"), shard, key, stat),
            ReaderIds.MSG_FREEZER);
        // cleanup
        stats.remove(key);
        createTime.remove(key);
      }
    }
  }

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
      var st = restorer.deserialize(OkapiIo.readBytes(is));
      stats.put(k, st);
    }
    OkapiIo.checkMagicNumber(is, MAGIC_NUMBER_END);
  }

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

  protected T getSecondlyStatistics(String timeSeries, long ts) {
    return stats.get(HashFns.secondlyBucket(timeSeries, ts));
  }

  protected T getMinutelyStatistics(String timeSeries, long ts) {
    return stats.get(HashFns.minutelyBucket(timeSeries, ts));
  }

  protected T getHourlyStatistics(String timeSeries, long ts) {
    return stats.get(HashFns.hourlyBucket(timeSeries, ts));
  }

  public byte[] getSerializedStats(String key) {
    var st = stats.get(key);
    return (st == null) ? null : st.serialize();
  }

  public Set<String> getKeys() {
    return Collections.unmodifiableSet(stats.keySet());
  }
}
