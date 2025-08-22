package org.okapi.metrics;

import static org.okapi.constants.Constants.N_SHARDS;

import com.google.common.base.Preconditions;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.okapi.clock.Clock;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.rollup.RollupSeries;
import org.okapi.metrics.stats.RollupSeriesRestorer;
import org.okapi.metrics.stats.Statistics;
import org.okapi.metrics.stats.StatisticsRestorer;

/**
 * Shard registry for in-memory rollup series. No global locks: - ConcurrentHashMap for shard
 * registry - RollupSeries & Statistics are thread-safe - Weakly consistent snapshots for periodic
 * persistence
 */
@Slf4j
public class ShardMap {

  private static final String SNAPSHOT_MAGIC = "ShardMapV1"; // v1 format

  // Atomic LSN watermark (monotonic, 1-based on apply())
  private final AtomicLong lsnCounter = new AtomicLong(0L);

  // Registry is atomically swappable on restore / targeted load
  private volatile Map<Integer, RollupSeries<Statistics>> shardMap;

  @Getter private Integer nsh;

  private final Clock clock;
  private final long admissionWindowMillis;
  private final Supplier<Statistics> newStatsSupplier;
  private final StatisticsRestorer<Statistics> statsRestorer;
  private final RollupSeriesRestorer<Statistics> restorer;

  // -------- Constructors --------

  public ShardMap(Clock clock, int admissionWindowHrs,
                  Supplier<Statistics> newStatsSupplier,
                  StatisticsRestorer<Statistics> statsRestorer,
                  RollupSeriesRestorer<Statistics> seriesRestorer
                  ) {
    this.clock = clock;
    Preconditions.checkArgument(
        admissionWindowHrs >= 1, "Admission window should atleast be an hour long.");
    this.admissionWindowMillis = TimeUnit.HOURS.toMillis(admissionWindowHrs);
    this.shardMap = new ConcurrentHashMap<>(500);
    this.nsh = N_SHARDS;
    this.newStatsSupplier = newStatsSupplier;
    this.statsRestorer = statsRestorer;
    this.restorer = seriesRestorer;
  }

  // -------- Basic ops --------

  /** Lazy create; safe under concurrency. */
  public RollupSeries<Statistics> get(int shardId) {
    return shardMap.computeIfAbsent(shardId, id -> new RollupSeries<>(statsRestorer, newStatsSupplier));
  }

  /** Weakly consistent view is fine. */
  public Set<Integer> shards() {
    return Collections.unmodifiableSet(shardMap.keySet());
  }

  /** Lazily initialize shards; do NOT precreate series. */
  public void reset(int shards) {
    this.nsh = shards;
    // atomic swap to a new empty registry
    this.shardMap = new ConcurrentHashMap<>(Math.max(16, shards * 2));
  }

  // -------- Snapshot / Restore --------

  /** Periodic snapshot with weak consistency; fsync for durability. */
  public void snapshot(Path checkpointPath) throws IOException {
    try (var fos = new FileOutputStream(checkpointPath.toFile())) {
      snapshot(fos);
      fos.getChannel().force(true); // fsync
    }
  }

  /** Weakly consistent snapshot of current shards. */
  public void snapshot(OutputStream os) throws IOException {
    // Take a shallow snapshot of entries to keep count/header consistent without locks
    var entries = new ArrayList<Map.Entry<Integer, RollupSeries<Statistics>>>(shardMap.size());
    shardMap.forEach((k, v) -> entries.add(Map.entry(k, v)));

    // Header
    OkapiIo.writeString(os, SNAPSHOT_MAGIC);
    OkapiIo.writeLong(os, lsnCounter.get());

    // Payload
    OkapiIo.writeInt(os, entries.size());
    for (var e : entries) {
      OkapiIo.writeInt(os, e.getKey());
      e.getValue().checkpoint(os); // RollupSeries already snapshots internally
    }
  }

  /** Atomic replace a single shard from a checkpoint file. */
  public void loadShard(int shard, Path checkpoint) throws StreamReadingException, IOException {
    var series = restorer.restore(checkpoint);
    var newMap = new ConcurrentHashMap<Integer, RollupSeries<Statistics>>(Math.max(16, shardMap.size() + 1));
    newMap.putAll(shardMap);
    newMap.put(shard, series);
    this.shardMap = newMap; // atomic publish
  }

  /** Atomic full reset from snapshot file (also restores LSN). */
  public void reset(Path checkpointPath) throws IOException, StreamReadingException {
    try (var fis = new FileInputStream(checkpointPath.toFile());
        var bis = new BufferedInputStream(fis);
        var dis = new DataInputStream(bis)) {

      // Header
      OkapiIo.checkMagicNumber(dis, SNAPSHOT_MAGIC);
      long snapLsn = dis.readLong();

      // Payload
      int shardCount = OkapiIo.readInt(dis);
      var newMap = new ConcurrentHashMap<Integer, RollupSeries<Statistics>>(Math.max(16, shardCount * 2));
      for (int i = 0; i < shardCount; i++) {
        int shardId = OkapiIo.readInt(dis);
        var series = restorer.restore(dis);
        newMap.put(shardId, series);
      }

      // Atomic publish
      this.shardMap = newMap;
      this.lsnCounter.set(snapLsn);
    }
  }

  // -------- Apply path --------

  /**
   * Atomically applies a batch to the given shard and returns the assigned LSN (monotonic,
   * 1-based). Throws OutsideWindowException if any timestamp is older than the admission window.
   */
  public long apply(int shardId, MetricsContext ctx, String path, long[] ts, float[] vals)
      throws OutsideWindowException {
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

    // Fast write path
    get(shardId).writeBatch(ctx, path, ts, vals);

    // On success, advance LSN
    return lsnCounter.incrementAndGet();
  }

  /** Set the LSN watermark (used after WAL recovery replay). */
  public void setWatermark(long lastAppliedLsn) {
    lsnCounter.set(lastAppliedLsn);
  }
}
