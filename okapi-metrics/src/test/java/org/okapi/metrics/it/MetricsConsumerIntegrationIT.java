package org.okapi.metrics.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.okapi.rest.metrics.SubmitMetricsRequestInternal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.clock.SystemClock;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.WriteBackRequest;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.rollup.RollupSeries;
import org.okapi.metrics.rollup.WriteBackSettings;
import org.okapi.metrics.service.ServiceController;
import org.okapi.metrics.service.runnables.WalBasedMetricsWriter;
import org.okapi.metrics.stats.*;
import org.okapi.wal.ManualLsnWalFramer;
import org.okapi.wal.PersistedLsnStore;
import org.okapi.wal.SpilloverWalWriter;
import org.okapi.wal.WalAllocator;
import org.okapi.wal.WalStreamerImpl;

public class MetricsConsumerIntegrationIT {
  StatisticsRestorer<Statistics> statsRestorer;
  Supplier<Statistics> statisticsSupplier;
  RollupSeriesRestorer<Statistics> restorer;
  Supplier<ShardMap> shardMapSupplier;
  Function<Integer, RollupSeries<Statistics>> seriesFunction;
  ScheduledExecutorService scheduledExecutorService;
  SharedMessageBox<WriteBackRequest> writeBackRequestSharedMessageBox;
  WriteBackSettings writeBackSettings;

  @TempDir Path walRoot;

  @BeforeEach
  public void setupSeries() {
    statsRestorer = new RolledupStatsRestorer();
    statisticsSupplier = new KllStatSupplier();
    seriesFunction = new RollupSeriesFn();
    writeBackSettings =
        new WriteBackSettings(Duration.of(1, ChronoUnit.SECONDS), new SystemClock());
    shardMapSupplier =
        () ->
            new ShardMap(
                new SystemClock(),
                1,
                seriesFunction,
                writeBackRequestSharedMessageBox,
                scheduledExecutorService,
                writeBackSettings);
  }

  @Test
  void startup_recovery_ingest_snapshot_restart_replay() throws Exception {
    // ----- Sharding & service stubs -----
    ShardsAndSeriesAssigner assigner =
        new ShardsAndSeriesAssigner() {
          @Override
          public int getShard(String path) {
            return 0;
          }

          @Override
          public String getNode(int shard) {
            return "self";
          }
        };
    ServiceController svc =
        new ServiceController() {
          volatile boolean allow = true;
          volatile boolean resumed = false;

          @Override
          public boolean canConsume() {
            return allow;
          }

          @Override
          public void pauseConsumer() {
            allow = false;
          }

          @Override
          public boolean resumeConsumer() {
            resumed = true;
            allow = true;
            return true;
          }

          @Override
          public boolean startProcess() {
            return true;
          }

          @Override
          public void stopProcess() {}

          @Override
          public boolean isProcessRunning() {
            return true;
          }
        };

    // ----- Common components -----
    ManualLsnWalFramer framer = new ManualLsnWalFramer(24);
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // ====== FIRST BOOT ======
    // Build runnable WITHOUT creating a writer yet (avoid taking write.lock prematurely)
    WalBasedMetricsWriter consumer =
        WalBasedMetricsWriter.builder()
            .shardMap(shardMapSupplier.get())
            .serviceController(svc)
            .self("self")
            .shardsAndSeriesAssigner(assigner)
            .walRoot(walRoot)
            .walWriter(null) // set after we create it with listener
            .walFramer(framer)
            .walStreamer(new WalStreamerImpl())
            .scheduler(scheduler)
            .snapshotDelay(Duration.ofMillis(50))
            .cleanerKeepLastK(1)
            .cleanerGrace(Duration.ZERO)
            .cleanerDryRun(false)
            .build();

    // Create writer WITH commit listener = consumer, then inject into runnable
    SpilloverWalWriter writerWithListener =
        new SpilloverWalWriter(
            new WalAllocator(walRoot),
            framer,
            64 * 1024,
            consumer,
            SpilloverWalWriter.FsyncPolicy.MANUAL,
            0,
            null);

    var writerField = WalBasedMetricsWriter.class.getDeclaredField("walWriter");
    writerField.setAccessible(true);
    writerField.set(consumer, writerWithListener);

    // Init & recover (empty WAL)
    consumer.init();
    assertThat(svc.canConsume()).isTrue();

    // Ingest some requests
    var t0 = System.currentTimeMillis();
    for (int i = 0; i < 5; i++) {
      consumer.onRequestArrive(
          req(
              "tenantA",
              "latency_ms",
              Map.of("route", "/api"),
              new long[] {t0 + 1_000, t0 + 2_000},
              new float[] {1f, 2f}));
    }

    // Allow snapshot to run
    Thread.sleep(200);

    // persisted watermark advanced
    PersistedLsnStore pls = PersistedLsnStore.open(walRoot);
    long persisted = pls.read();
    int writes = 5;
    long expectedHighestLsn = writes - 1; // 0-based LSNs: 0..4
    assertThat(persisted).isGreaterThanOrEqualTo(expectedHighestLsn);
    // IMPORTANT: close writer to release write.lock before "restart"
    writerWithListener.close();

    // ====== SECOND BOOT (simulated restart) ======
    WalBasedMetricsWriter consumer2 =
        WalBasedMetricsWriter.builder()
            .shardMap(shardMapSupplier.get())
            .serviceController(svc)
            .self("self")
            .shardsAndSeriesAssigner(assigner)
            .walRoot(walRoot)
            .walWriter(null) // will set after creating writer
            .walFramer(framer)
            .walStreamer(new WalStreamerImpl())
            .scheduler(scheduler)
            .snapshotDelay(Duration.ofMillis(50))
            .cleanerKeepLastK(1)
            .cleanerGrace(Duration.ZERO)
            .cleanerDryRun(false)
            .build();

    SpilloverWalWriter writer2WithListener =
        new SpilloverWalWriter(
            new WalAllocator(walRoot),
            framer,
            64 * 1024,
            consumer2,
            SpilloverWalWriter.FsyncPolicy.MANUAL,
            0,
            null);
    writerField.set(consumer2, writer2WithListener);

    consumer2.init();
    assertThat(svc.canConsume()).isTrue();

    // Clean up
    writer2WithListener.close();
    scheduler.shutdownNow();
  }

  private static SubmitMetricsRequestInternal req(
      String tenant, String metric, Map<String, String> tags, long[] ts, float[] vals) {
    SubmitMetricsRequestInternal r = new SubmitMetricsRequestInternal();
    r.setTenantId(tenant);
    r.setMetricName(metric);
    r.setTags(tags);
    r.setTs(ts);
    r.setValues(vals);
    return r;
  }
}
