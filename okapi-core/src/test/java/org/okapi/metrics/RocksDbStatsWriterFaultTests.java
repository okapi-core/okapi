package org.okapi.metrics;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.clock.SystemClock;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.rocks.RocksDbWriter;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.rollup.HashFns;
import org.okapi.metrics.rollup.WriteBackSettings;
import org.okapi.metrics.stats.*;

public class RocksDbStatsWriterFaultTests {

  class FaultyMerger implements Merger<UpdatableStatistics> {

    int called = 0;
    @Override
    public UpdatableStatistics merge(UpdatableStatistics A, UpdatableStatistics B) {
      called += 1;
      throw new IllegalArgumentException();
    }
  }

  class FaultyRocksStore extends RocksStore {

    public FaultyRocksStore() throws IOException {
      super();
    }
    public RocksDbWriter rocksWriter(Path path) throws IOException {
      throw new IOException("Canned IO exception.");
    }
  }

  @TempDir Path hourlyRoot;
  @TempDir Path shardPkgRoot;
  @TempDir Path parquetRoot;
  @TempDir Path shardAssetsRoot;

  ScheduledExecutorService scheduledExecutorService;
  RocksStore rocksStore;
  WriteBackSettings writeBackSettings;
  SharedMessageBox<WriteBackRequest> messageBox;
  StatisticsRestorer<UpdatableStatistics> restorer;
  PathRegistry pathRegistry;
  RocksDbStatsWriter rocksDbStatsWriter;

  // helpers
  Supplier<UpdatableStatistics> statsSupplier;

  @BeforeEach
  public void setup() throws IOException, StatisticsFrozenException, InterruptedException {
    scheduledExecutorService = Executors.newScheduledThreadPool(1);
    rocksStore = new RocksStore();
    writeBackSettings =
        new WriteBackSettings(Duration.of(100, ChronoUnit.MILLIS), new SystemClock());
    messageBox = new SharedMessageBox<>(10);
    restorer = new WritableRestorer();
    pathRegistry =
        new PathRegistryImpl(
            hourlyRoot, shardPkgRoot, parquetRoot, shardAssetsRoot, new ReentrantReadWriteLock());
    statsSupplier = new KllStatSupplier();
    pushStats();
  }
  
  public void pushStats() throws StatisticsFrozenException, InterruptedException {
    var t0 = System.currentTimeMillis();
    var key = HashFns.minutelyBucket("mp{}", t0);
    var stats = statsSupplier.get();
    var ctx = MetricsContext.createContext("id");
    stats.update(ctx, 0.1f);
    messageBox.push(new WriteBackRequest(
            ctx,
            0, key, stats
    ), this.getClass().getSimpleName());

  }

  @Test
  public void testWritesContinueIfNoMerge() throws IOException, StatisticsFrozenException, InterruptedException {
    var merger = new FaultyMerger();
    rocksDbStatsWriter =
        new RocksDbStatsWriter(messageBox, restorer, 
                merger, pathRegistry);
    rocksDbStatsWriter.setRocksStore(rocksStore);
    rocksDbStatsWriter.once();

    pushStats();
    assertFalse(messageBox.isEmpty());
    rocksDbStatsWriter.once();
    
    assertEquals(1, merger.called);
  }

  @Test
  public void testOnceFailsIfDbNotFound() throws IOException {
    var store = new FaultyRocksStore();
    rocksDbStatsWriter =
        new RocksDbStatsWriter(
            messageBox, restorer, new RolledupMergerStrategy(), pathRegistry);
    rocksDbStatsWriter.setRocksStore(store);
    rocksDbStatsWriter.once();
  }
}
