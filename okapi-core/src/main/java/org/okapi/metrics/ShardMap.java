package org.okapi.metrics;

import static org.okapi.constants.Constants.N_SHARDS;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.okapi.clock.Clock;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.paths.PathSet;
import org.okapi.metrics.rollup.RollupSeries;
import org.okapi.metrics.rollup.WriteBackSettings;
import org.okapi.metrics.stats.*;

/**
 * Shard registry for in-memory rollup series. No global locks: - ConcurrentHashMap for shard
 * registry - RollupSeries & Statistics are thread-safe - Weakly consistent snapshots for periodic
 * persistence
 */
@Slf4j
public class ShardMap {

  // Registry is atomically swappable on restore / targeted load
  private volatile Map<Integer, RollupSeries<UpdatableStatistics>> shardMap;
  @Getter private Integer nsh;
  private final Clock clock;
  private final long admissionWindowMillis;
  private final Function<Integer, RollupSeries<UpdatableStatistics>> seriesSupplier;
  SharedMessageBox<WriteBackRequest> messageBox;
  ScheduledExecutorService scheduledExecutorService;
  WriteBackSettings writeBackSettings;
  PathSet pathSet;

  public ShardMap(
      Clock clock,
      int admissionWindowHrs,
      Function<Integer, RollupSeries<UpdatableStatistics>> seriesSupplier,
      SharedMessageBox<WriteBackRequest> messageBox,
      ScheduledExecutorService scheduledExecutorService,
      WriteBackSettings writeBackSettings,
      PathSet pathSet) {
    this.clock = clock;
    Preconditions.checkArgument(
        admissionWindowHrs >= 1, "Admission window should atleast be an hour long.");
    this.admissionWindowMillis = TimeUnit.HOURS.toMillis(admissionWindowHrs);
    this.shardMap = new ConcurrentHashMap<>(N_SHARDS);
    this.nsh = N_SHARDS;
    this.seriesSupplier = seriesSupplier;
    this.messageBox = messageBox;
    this.scheduledExecutorService = scheduledExecutorService;
    this.writeBackSettings = writeBackSettings;
    this.pathSet = pathSet;
  }

  protected RollupSeries<UpdatableStatistics> get(int shardId) {
    return shardMap.computeIfAbsent(
        shardId,
        (sh) -> {
          var newSeries = seriesSupplier.apply(sh);
          newSeries.startFreezing(messageBox, scheduledExecutorService, this.writeBackSettings);
          return newSeries;
        });
  }

  public Set<Integer> shards() {
    return Collections.unmodifiableSet(shardMap.keySet());
  }

  /**
   * Atomically applies a batch to the given shard and returns the assigned LSN (monotonic,
   * 1-based). Throws OutsideWindowException if any timestamp is older than the admission window.
   */
  public long apply(int shardId, MetricsContext ctx, String path, long[] ts, float[] vals)
      throws OutsideWindowException, StatisticsFrozenException, InterruptedException, IOException {
    if (ts == null || vals == null) {
      throw new IllegalArgumentException("ts/vals cannot be null");
    }
    if (ts.length != vals.length) {
      throw new IllegalArgumentException(
          "ts and vals length mismatch: " + ts.length + " vs " + vals.length);
    }

    // Admission window check (optional if window == 0)
    if (admissionWindowMillis > 0) {
      final long now = clock.currentTimeMillis();
      final long cutoff = now - admissionWindowMillis;
      for (long t : ts) {
        if (t < cutoff) {
          throw new OutsideWindowException("Timestamp " + t + " is outside the admission window");
        }
      }
    }

    var series = get(shardId);
    pathSet.add(shardId, path);
    series.writeBatch(ctx, path, ts, vals);
    return 0;
  }

  @VisibleForTesting
  public void forciblyApply(int shardId, MetricsContext ctx, String path, long[] ts, float[] vals)
      throws IOException, StatisticsFrozenException, InterruptedException {
    var series = get(shardId);
    pathSet.add(shardId, path);
    series.writeBatch(ctx, path, ts, vals);
  }

  public void flushAll() throws InterruptedException {
    for (var series : shardMap.values()) {
      series.flush();
    }
  }
}
