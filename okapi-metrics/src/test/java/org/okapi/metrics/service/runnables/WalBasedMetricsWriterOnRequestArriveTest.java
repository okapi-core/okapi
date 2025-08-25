package org.okapi.metrics.service.runnables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.okapi.rest.metrics.SubmitMetricsRequestInternal;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.clock.Clock;
import org.okapi.clock.SystemClock;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.WriteBackRequest;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.rollup.RollupSeries;
import org.okapi.metrics.rollup.WriteBackSettings;
import org.okapi.metrics.service.ServiceController;
import org.okapi.metrics.stats.*;
import org.okapi.wal.ManualLsnWalFramer;
import org.okapi.wal.SpilloverWalWriter;
import org.okapi.wal.Wal.Lsn;
import org.okapi.wal.WalAllocator;
import org.okapi.wal.WalStreamerImpl;

class WalBasedMetricsWriterOnRequestArriveTest {

  @TempDir Path walRoot;

  Supplier<ShardMap> shardMapSupplier;
  Function<Integer, RollupSeries<Statistics>> seriesFunction;
  StatisticsRestorer<Statistics> statsRestorer;
  Supplier<Statistics> statisticsSupplier;
  RollupSeriesRestorer<Statistics> restorer;

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final SharedMessageBox<WriteBackRequest> requestSharedMessageBox =
      new SharedMessageBox<>(1000);
  Clock clock;
  WriteBackSettings writeBackSettings;

  @BeforeEach
  void setup() {
    statsRestorer = new RolledupStatsRestorer();
    statisticsSupplier = new KllStatSupplier();
    seriesFunction = new RollupSeriesFn();
    restorer = new RolledUpSeriesRestorer(seriesFunction);
    clock = new SystemClock();
    writeBackSettings = new WriteBackSettings(Duration.of(1, ChronoUnit.SECONDS), clock);
    shardMapSupplier =
        () ->
            new ShardMap(
                new SystemClock(),
                1,
                seriesFunction,
                requestSharedMessageBox,
                scheduler,
                writeBackSettings);
  }

  @AfterEach
  void tearDown() {
    scheduler.shutdownNow();
  }

  @Test
  void rejectsNullRequest() throws IOException {
    WalBasedMetricsWriter r = baseRunnable(walRoot);
    assertThatThrownBy(() -> r.onRequestArrive(null))
        .isInstanceOf(org.okapi.exceptions.BadRequestException.class);
  }

  @Test
  void rejectsWhenServicePaused() throws IOException {
    ServiceController svc =
        new ServiceController() {
          @Override
          public boolean canConsume() {
            return false;
          }

          @Override
          public void pauseConsumer() {}

          @Override
          public boolean resumeConsumer() {
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

    var runnable = buildRunnable(walRoot, svc, assigner);
    SubmitMetricsRequestInternal req =
        req("t", "m", Map.of(), new long[] {System.currentTimeMillis()}, new float[] {1f});

    assertThatThrownBy(() -> runnable.onRequestArrive(req))
        .isInstanceOf(org.okapi.exceptions.BadRequestException.class);
  }

  @Test
  void rejectsIfWrongNode() throws Exception {
    ServiceController svc = okSvc();
    ShardsAndSeriesAssigner assigner =
        new ShardsAndSeriesAssigner() {
          @Override
          public int getShard(String path) {
            return 0;
          }

          @Override
          public String getNode(int shard) {
            return "other-node";
          } // not "self"
        };
    var runnable = buildRunnable(walRoot, svc, assigner);
    SubmitMetricsRequestInternal req =
        req("t", "m", Map.of(), new long[] {System.currentTimeMillis()}, new float[] {1f});

    assertThatThrownBy(() -> runnable.onRequestArrive(req))
        .isInstanceOf(org.okapi.exceptions.BadRequestException.class);
  }

  @Test
  void outsideWindow_throws_andDoesNotWriteWal() throws Exception {
    ServiceController svc = okSvc();
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

    // Use a fixed clock so we can craft an out-of-window timestamp precisely
    long fixedNow = 10_000_000_000L; // arbitrary ms
    ShardMap shardMap = shardMapSupplier.get();

    WalBasedMetricsWriter runnable = buildRunnableWithShardMap(walRoot, svc, assigner, shardMap);

    long outside = fixedNow - (org.okapi.metrics.rollup.RollupSeries.ADMISSION_WINDOW + 1_000L);
    SubmitMetricsRequestInternal req =
        req("tenant", "m", Map.of("k", "v"), new long[] {outside}, new float[] {1f});

    // Expect OutsideWindowException from ShardMap.apply(...) path
    assertThatThrownBy(() -> runnable.onRequestArrive(req))
        .isInstanceOf(OutsideWindowException.class);

    // Ensure no WAL payload got written: either no segment file, or segments are empty
    boolean anyNonEmptySegment =
        Files.list(walRoot)
            .filter(p -> p.getFileName().toString().matches("^wal_\\d{10}\\.segment$"))
            .anyMatch(
                p -> {
                  try {
                    return Files.size(p) > 0;
                  } catch (Exception e) {
                    return true;
                  } // treat errors as non-empty to be conservative
                });

    assertThat(anyNonEmptySegment).isFalse();

    // Clean up writer to release locks if it was created
    getWriter(runnable).close();
  }

  @Test
  void success_writesWal_withExactLsnFromShardMap() throws Exception {
    ServiceController svc = okSvc();
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
    var runnable = buildRunnable(walRoot, svc, assigner);

    long now = System.currentTimeMillis();
    SubmitMetricsRequestInternal req =
        req(
            "tenant",
            "latency_ms",
            Map.of("route", "/api"),
            new long[] {now, now + 1},
            new float[] {1f, 2f});

    runnable.onRequestArrive(req);

    // Close writer (release handle and ensure bytes are visible)
    getWriter(runnable).close();

    // There should be exactly one segment; parse its first record's LSN
    Path seg =
        Files.list(walRoot)
            .filter(p -> p.getFileName().toString().matches("^wal_\\d{10}\\.segment$"))
            .sorted(Comparator.naturalOrder())
            .findFirst()
            .orElseThrow();

    long lsnFromWal = readFirstLsn(seg);
    // Since ShardMap starts at 1-based LSNs, first applied batch gets LSN=1
    assertThat(lsnFromWal).isEqualTo(1L);
  }

  // ---- helpers ----

  private WalBasedMetricsWriter buildRunnable(
      Path root, ServiceController svc, ShardsAndSeriesAssigner assigner) throws IOException {
    ManualLsnWalFramer framer = new ManualLsnWalFramer(24);
    WalAllocator allocator = new WalAllocator(root);

    // Build runnable without writer, then attach writer with listener=runnable
    WalBasedMetricsWriter runnable =
        WalBasedMetricsWriter.builder()
            .shardMap(shardMapSupplier.get())
            .serviceController(svc)
            .self("self")
            .shardsAndSeriesAssigner(assigner)
            .walRoot(root)
            .walWriter(null)
            .walFramer(framer)
            .walStreamer(new WalStreamerImpl())
            .scheduler(scheduler)
            .snapshotDelay(Duration.ofMillis(500))
            .cleanerKeepLastK(1)
            .cleanerGrace(Duration.ZERO)
            .cleanerDryRun(false)
            .build();

    SpilloverWalWriter writer =
        new SpilloverWalWriter(
            allocator, framer, 64 * 1024, runnable, SpilloverWalWriter.FsyncPolicy.MANUAL, 0, null);

    setWriter(runnable, writer);
    return runnable;
  }

  private WalBasedMetricsWriter buildRunnableWithShardMap(
      Path root, ServiceController svc, ShardsAndSeriesAssigner assigner, ShardMap shardMap)
      throws IOException {
    ManualLsnWalFramer framer = new ManualLsnWalFramer(24);
    WalAllocator allocator = new WalAllocator(root);

    WalBasedMetricsWriter runnable =
        WalBasedMetricsWriter.builder()
            .shardMap(shardMap)
            .serviceController(svc)
            .self("self")
            .shardsAndSeriesAssigner(assigner)
            .walRoot(root)
            .walWriter(null)
            .walFramer(framer)
            .walStreamer(new WalStreamerImpl())
            .scheduler(scheduler)
            .snapshotDelay(Duration.ofMillis(500))
            .cleanerKeepLastK(1)
            .cleanerGrace(Duration.ZERO)
            .cleanerDryRun(false)
            .build();

    SpilloverWalWriter writer =
        new SpilloverWalWriter(
            allocator, framer, 64 * 1024, runnable, SpilloverWalWriter.FsyncPolicy.MANUAL, 0, null);

    setWriter(runnable, writer);
    return runnable;
  }

  private WalBasedMetricsWriter baseRunnable(Path root) throws IOException {
    return buildRunnable(
        root,
        okSvc(),
        new ShardsAndSeriesAssigner() {
          @Override
          public int getShard(String path) {
            return 0;
          }

          @Override
          public String getNode(int shard) {
            return "self";
          }
        });
  }

  private static ServiceController okSvc() {
    return new ServiceController() {
      @Override
      public boolean canConsume() {
        return true;
      }

      @Override
      public void pauseConsumer() {}

      @Override
      public boolean resumeConsumer() {
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

  private static void setWriter(WalBasedMetricsWriter r, SpilloverWalWriter w) {
    try {
      var f = WalBasedMetricsWriter.class.getDeclaredField("walWriter");
      f.setAccessible(true);
      f.set(r, w);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static SpilloverWalWriter getWriter(WalBasedMetricsWriter r) {
    try {
      var f = WalBasedMetricsWriter.class.getDeclaredField("walWriter");
      f.setAccessible(true);
      return (SpilloverWalWriter) f.get(r);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static long readFirstLsn(Path segment) throws Exception {
    try (FileChannel ch = FileChannel.open(segment, StandardOpenOption.READ)) {
      long pos = 0;
      // [len][LSN]
      int l1 = readInt(ch, pos);
      pos += 4;
      byte[] lsnBytes = readBytes(ch, pos, l1);
      pos += l1;
      return Lsn.parseFrom(lsnBytes).getN();
    }
  }

  private static int readInt(FileChannel ch, long pos) throws Exception {
    ByteBuffer b = ByteBuffer.allocate(4);
    int n = ch.read(b, pos);
    if (n < 4) throw new IllegalStateException("short read");
    b.flip();
    return b.getInt();
  }

  private static byte[] readBytes(FileChannel ch, long pos, int len) throws Exception {
    ByteBuffer b = ByteBuffer.allocate(len);
    int read = 0;
    while (b.hasRemaining()) {
      int n = ch.read(b, pos + read);
      if (n < 0) break;
      read += n;
    }
    if (read != len) throw new IllegalStateException("short read");
    return b.array();
  }
}
