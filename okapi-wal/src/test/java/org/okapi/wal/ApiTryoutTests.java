package org.okapi.wal;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ApiTryoutTests {

  @Test
  @Disabled
  public void testWalIntegration() throws IOException {
    var root = Path.of("./wal-record");
    var walAdapter = MetricWalRecordAdapter.create();
    var walAllocator = new WalAllocator(root);
    var longSupplier = new OffsetLsnSupplier();
    var walFramer = new TupleWalFramer(longSupplier, 4);
    var maxSegSize = 1024L * 1024 * 1024;
    WalCommitListener commitListener =
        new WalCommitListener() {
          @Override
          public void onWalCommit(WalCommitContext ctx) {
            System.out.println("ctx: " + ctx);
          }
        };
    SpilloverWalWriter.FsyncPolicy fsyncPolicy = SpilloverWalWriter.FsyncPolicy.INTERVAL;
    long fsyncBytesThreshold = 1024L * 1024;
    Duration fsyncInterval = Duration.of(2, ChronoUnit.SECONDS);
    var walWriter =
        new SpilloverWalWriter(
            walAllocator,
            walFramer,
            maxSegSize,
            commitListener,
            fsyncPolicy,
            fsyncBytesThreshold,
            fsyncInterval);
    var scheduler = Executors.newScheduledThreadPool(20);
    var walBatchWriter =
        new WalBatchWriter<>(
            walAdapter,
            walWriter,
            walFramer,
            scheduler,
            Duration.of(2, ChronoUnit.SECONDS),
            100,
            1024 * 1024);
    var t0 = System.currentTimeMillis();
    var ts = Arrays.asList(t0, t0 + 1000, t0 + 2000);
    var vals = Arrays.asList(0.1f, 0.2f, 0.3f);
    var event =
        Wal.MetricEvent.newBuilder()
            .addAllTs(ts)
            .addAllVals(vals)
            .setName("latency")
            .putTags("serivce", "a")
            .build();
    var eventSize = event.getSerializedSize();
    walBatchWriter.consume(event, eventSize);
    walBatchWriter.consume(event, eventSize);
    walBatchWriter.close();
  }

  @Test
  @Disabled
  public void testWalConsumer() throws IOException {
    var root = Path.of("./wal-record");
    WalStreamer walStreamer = new WalStreamerImpl();
    var consumer =
        new WalStreamConsumer() {

          @Override
          public void consume(long lsn, Wal.WalRecord record) throws Exception {
            System.out.println("LSN = " + lsn + " record = " + record);
          }

          @Override
          public long lastAppliedLsn() {
            return Long.MIN_VALUE;
          }
        };
    walStreamer.stream(root, consumer, WalStreamer.Options.builder().build());
  }
}
