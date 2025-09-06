package org.okapi.metrics;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.Statistics;
import org.okapi.clock.SystemClock;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.rollup.HashFns;
import org.okapi.metrics.rollup.WriteBackSettings;
import org.okapi.metrics.stats.*;

public class RocksDbStatsWriterTests {

  @TempDir Path hourlyRoot;
  @TempDir Path shardPkgRoot;
  @TempDir Path parquetRoot;
  @TempDir Path shardAssetsRoot;

  ScheduledExecutorService scheduledExecutorService;
  RocksStore rocksStore;
  WriteBackSettings writeBackSettings;
  SharedMessageBox<WriteBackRequest> messageBox;
  StatisticsRestorer<Statistics> restorer;
  StatisticsRestorer<UpdatableStatistics> writableRestorer;
  PathRegistry pathRegistry;
  RocksDbStatsWriter rocksDbStatsWriter;

  // helpers
  Supplier<UpdatableStatistics> statsSupplier;

  @BeforeEach
  public void setup() throws IOException {
    scheduledExecutorService = Executors.newScheduledThreadPool(1);
    rocksStore = new RocksStore();
    writeBackSettings =
        new WriteBackSettings(Duration.of(100, ChronoUnit.MILLIS), new SystemClock());
    messageBox = new SharedMessageBox<>(10);
    restorer = new ReadonlyRestorer();
    writableRestorer = new WritableRestorer();
    pathRegistry =
        new PathRegistryImpl(
            hourlyRoot, shardPkgRoot, parquetRoot, shardAssetsRoot, new ReentrantReadWriteLock());
    rocksDbStatsWriter =
        new RocksDbStatsWriter(
            messageBox,
                writableRestorer,
                new RolledupMergerStrategy(), pathRegistry);
    statsSupplier = new KllStatSupplier();
  }

  @Test
  public void testWriteOnce_noMessages() {
    rocksDbStatsWriter.once();
  }

  @Test
  public void testWriteOnceDrainsQueue() throws StatisticsFrozenException, InterruptedException {
    var t0 = System.currentTimeMillis();
    var key = HashFns.minutelyBucket("mp{}", t0);
    var stats = statsSupplier.get();
    var ctx = MetricsContext.createContext("id");
    stats.update(ctx, 0.1f);
    messageBox.push(new WriteBackRequest(
            ctx,
            0, key, stats
    ));
    var stats2 = statsSupplier.get();
    var key2 = HashFns.minutelyBucket("mp-2{}", t0);
    stats2.update(ctx, 0.1f);
    messageBox.push(new WriteBackRequest(
            ctx,
            0, key2, stats2
    ));
    rocksDbStatsWriter.setRocksStore(rocksStore);
    rocksDbStatsWriter.once();
    Assertions.assertTrue(messageBox.isEmpty());
  }

}
