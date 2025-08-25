package org.okapi.metrics.service.runnables;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.okapi.metrics.common.MetricPaths.convertToPath;
import static org.okapi.metrics.common.MetricPaths.getMetricsContext;

import com.okapi.rest.metrics.SubmitMetricsRequestInternal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.service.ServiceController;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.metrics.wal.MetricsWalStreamConsumer;
import org.okapi.wal.*;
import org.okapi.wal.Wal.MetricEvent;
import org.okapi.wal.Wal.MetricEventBatch;
import org.okapi.wal.Wal.WalRecord;

/**
 * Ingest flow: 1) onRequestArrive: validate + assign, apply to ShardMap (fail → no WAL). 2) If
 * ShardMap write ok, append to WAL using EXACT LSN returned by counter. 3) On WAL commit, debounce
 * a snapshot (configurable delay). Snapshot writes ShardMap to "shardMap.snapshot", updates
 * persisted watermark via PersistedLsnStore, then triggers cleaner. 4) On startup: restore snapshot
 * (if exists), run STRICT recovery, stream WAL from watermark, then resume consumption via
 * ServiceController.
 */
@Slf4j
@Builder
@AllArgsConstructor
public class WalBasedMetricsWriter implements WalCommitListener, MetricsWriter {

  // Core dependencies
  final ShardMap shardMap;
  final ServiceController serviceController;
  final String self;
  @Setter ShardsAndSeriesAssigner shardsAndSeriesAssigner;

  // WAL wiring
  final Path walRoot;
  @Setter SpilloverWalWriter walWriter; // constructed with ManualLsnWalFramer + commitListener=this
  final ManualLsnWalFramer walFramer;
  final WalStreamer walStreamer;

  // Snapshot/Cleaner
  final ScheduledExecutorService scheduler;
  final Duration snapshotDelay; // X minutes (configurable)
  final int cleanerKeepLastK;
  final Duration cleanerGrace;
  final boolean cleanerDryRun;

  // Internal state
  private final AtomicBoolean snapshotScheduled = new AtomicBoolean(false);
  private final AtomicBoolean snapshotRunning = new AtomicBoolean(false);
  private volatile long lastCommittedLsn = -1L;
  private volatile ScheduledFuture<?> snapshotFuture;

  // ----- Startup / Recovery -----
  private final AtomicBoolean initialized = new AtomicBoolean(false);

  /**
   * Initialize on process start: restore ShardMap, recover WAL, stream pending, resume consumer.
   */
  @Override
  public void init() throws IOException {
    // Restore ShardMap from snapshot if present
    Path snapshot = walRoot.resolve("shardMap.snapshot");
    if (Files.exists(snapshot)) {
      try {
        shardMap.reset(snapshot);
        log.info("ShardMap restored from {}", snapshot);
      } catch (Exception e) {
        log.warn("ShardMap snapshot restore failed (continuing with empty map): {}", e.toString());
      }
    }

    // Read persisted watermark (source of truth)
    PersistedLsnStore pls = PersistedLsnStore.open(walRoot);
    long watermark = pls.read();
    if (watermark < 0) watermark = -1L;

    // Recover WAL (strict) and stream pending into ShardMap
    WalStreamer.Options opts = new WalStreamer.Options();
    opts.runRecovery = true;
    opts.verifyCrc = true;
    opts.fenceToPersistedLsn = false;

    MetricsWalStreamConsumer consumer =
        new MetricsWalStreamConsumer(shardMap, shardsAndSeriesAssigner, self, watermark);
    WalStreamer.Result res = walStreamer.stream(walRoot, consumer, opts);
    lastCommittedLsn = consumer.lastAppliedLsn();

    // Keep ShardMap's counter in sync with recovery
    shardMap.setWatermark(lastCommittedLsn);
    log.info(
        "Recovery complete: delivered={}, lastLSN={}, segments={}",
        res.recordsDelivered,
        res.lastDeliveredLsn,
        res.segmentsVisited);
    initialized.set(true);
  }

  // ----- Request handling -----

  @Override
  public void onRequestArrive(SubmitMetricsRequestInternal request)
      throws BadRequestException, OutsideWindowException, InterruptedException, StatisticsFrozenException {
    if (request == null) {
      throw new BadRequestException("Request is null.");
    }
    if (!serviceController.canConsume()) {
      throw new BadRequestException("Cannot process request as cluster is possibly resharding.");
    }
    if (shardsAndSeriesAssigner == null) {
      throw new BadRequestException("Request sent before sharding is ready.");
    }

    // Shard / node routing
    final String path = convertToPath(request);
    final int shard = shardsAndSeriesAssigner.getShard(path);
    final String node = shardsAndSeriesAssigner.getNode(shard);
    if (!self.equals(node)) {
      throw new BadRequestException("Metric doesn't belong to this node.");
    }

    // Build context once
    final var ctx = getMetricsContext(request);

    // 1) Apply to in-memory store and get the EXACT monotonic LSN from ShardMap.
    //    If this throws (e.g., OutsideWindowException), DO NOT write to WAL.
    final long lsn = shardMap.apply(shard, ctx, path, request.getTs(), request.getValues());

    // 2) Prepare WAL payload for the same request (metrics batch).
    final MetricEvent.Builder ev =
        MetricEvent.newBuilder().setName(request.getMetricName()).putAllTags(request.getTags());
    for (long t : request.getTs()) ev.addTs(t);
    for (float v : request.getValues()) ev.addVals(v);
    final WalRecord walRecord =
        WalRecord.newBuilder()
            .setEvent(MetricEventBatch.newBuilder().addEvents(ev).build())
            .build();

    // 3) Append to WAL using the EXACT same LSN via ManualLsnWalFramer.
    walFramer.setNextLsn(lsn);
    try {
      walWriter.write(walRecord); // onWalCommit(...) will fire on success and schedule snapshot
    } catch (IOException ioe) {
      // WAL append failed after ShardMap apply. Client will retry; double-apply mitigation can be
      // added later.
      throw new BadRequestException("WAL write failed: " + ioe.getMessage());
    }
  }

  @Override
  public boolean isReady() {
    return initialized.get();
  }

  @Override
  public void onWalCommit(WalCommitContext ctx) {
    // Update last committed LSN and schedule snapshot (debounced)
    lastCommittedLsn = ctx.getLsn();
    scheduleSnapshotDebounced();
  }

  private void scheduleSnapshotDebounced() {
    if (snapshotScheduled.compareAndSet(false, true)) {
      long delayMs = Math.max(0L, snapshotDelay.toMillis());
      snapshotFuture = scheduler.schedule(this::runSnapshotOnceSafe, delayMs, MILLISECONDS);
    }
  }

  private void runSnapshotOnceSafe() {
    if (!snapshotRunning.compareAndSet(false, true)) {
      // already running; skip this run
      snapshotScheduled.set(false);
      return;
    }
    try {
      runSnapshotOnce();
    } catch (Throwable t) {
      log.error("Snapshot failed: {}", t.toString(), t);
    } finally {
      snapshotRunning.set(false);
      snapshotScheduled.set(false);
    }
  }

  private void runSnapshotOnce() throws IOException {
    // 1) Write ShardMap snapshot
    Path snapshot = walRoot.resolve("shardMap.snapshot");
    shardMap.snapshot(snapshot);

    // 2) Update persisted watermark (fsynced by PersistedLsnStore)
    PersistedLsnStore pls = PersistedLsnStore.open(walRoot);
    pls.updateIfGreater(lastCommittedLsn);

    // 3) Cleaner: delete sealed segments eligible by watermark
    WalCleaner cleaner =
        new WalCleaner(
            walRoot, java.time.Clock.systemUTC(), cleanerGrace, cleanerKeepLastK, cleanerDryRun);
    cleaner.run();

    log.info(
        "Snapshot completed at LSN={}, written={}, cleaner invoked", lastCommittedLsn, snapshot);
  }
}
