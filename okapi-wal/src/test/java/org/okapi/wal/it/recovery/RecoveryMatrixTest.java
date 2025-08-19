package org.okapi.wal.it.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.wal.*;
import org.okapi.wal.Wal.MetricEvent;
import org.okapi.wal.Wal.MetricEventBatch;
import org.okapi.wal.Wal.WalRecord;
import org.okapi.wal.it.env.WalTestEnv;
import org.okapi.wal.it.env.WriterBuilder;
import org.okapi.wal.it.faults.CrashInjector;
import org.okapi.wal.it.faults.CrashPoint;
import org.okapi.wal.it.faults.FramerWithFaults;
import org.okapi.wal.it.observers.CommitHookRecorder;

public class RecoveryMatrixTest {

  @TempDir Path tmp;

  @Test
  void crashDuringPayload_STRICT_truncatesTail_andAllowsResume() throws Exception {
    WalTestEnv env = new WalTestEnv(tmp);
    WalAllocator alloc = env.allocator();

    // Arm crash on the 3rd DURING_PAYLOAD (so first two writes succeed)
    CrashInjector inj = new CrashInjector();
    inj.arm(CrashPoint.DURING_PAYLOAD, 3);
    AtomicLong lsn = new AtomicLong(0);
    WalFramer framer = new FramerWithFaults(24, lsn, inj);
    CommitHookRecorder hook = new CommitHookRecorder();

    SpilloverWalWriter writer =
        new WriterBuilder()
            .allocator(alloc)
            .framer(framer)
            .maxSegmentSize(1_000_000)
            .commitListener(hook)
            .build();

    // write 2 complete records
    writer.write(recordWithNMetrics(2));
    writer.write(recordWithNMetrics(2));

    // 3rd write crashes during payload
    assertThatThrownBy(() -> writer.write(recordWithNMetrics(2)))
        .isInstanceOf(RuntimeException.class);
    // simulate crash: do not close(), so write.lock remains
    long beforeRecoverySize = Files.size(env.activeSegmentPath());

    // Recover STRICT (truncate)
    WalRecoveryScanner.Result res =
        env.recoveryScanner().recover(tmp, WalRecoveryScanner.TailPolicy.STRICT_TRUNCATE);
    long afterRecoverySize = Files.size(env.activeSegmentPath());
    assertThat(afterRecoverySize).isLessThanOrEqualTo(beforeRecoverySize);
    assertThat(res.truncateOffset).isGreaterThanOrEqualTo(0);

    // Force unlock as an operator would, then resume writing with a fresh writer and a safe framer
    env.forceUnlocks();
    CommitHookRecorder hook2 = new CommitHookRecorder();
    WalFramer framerSafe = new org.okapi.wal.FakeFramer(24, lsn); // reuse test helper
    SpilloverWalWriter writer2 =
        new WriterBuilder()
            .allocator(env.allocator())
            .framer(framerSafe)
            .maxSegmentSize(1_000_000)
            .commitListener(hook2)
            .build();

    writer2.write(recordWithNMetrics(1));
    writer2.close();

    assertThat(hook.events().size()).isEqualTo(2); // first two commits only
    assertThat(hook2.events().size()).isEqualTo(1);
  }

  @Test
  void recoveryReportsHighestValidLsn_salvageMode() throws Exception {
    WalTestEnv env = new WalTestEnv(tmp);
    WalAllocator alloc = env.allocator();

    // Crash on 3rd write after CRC but before payload (so first two are valid)
    AtomicLong lsn = new AtomicLong(0);
    CrashInjector inj = new CrashInjector();
    inj.arm(CrashPoint.AFTER_CRC_BEFORE_PAYLOAD, 3);
    WalFramer framer = new FramerWithFaults(24, lsn, inj);

    SpilloverWalWriter writer =
        new WriterBuilder().allocator(alloc).framer(framer).maxSegmentSize(1_000_000).build();

    writer.write(recordWithNMetrics(1));
    writer.write(recordWithNMetrics(1));
    assertThatThrownBy(() -> writer.write(recordWithNMetrics(1)))
        .isInstanceOf(RuntimeException.class);

    WalRecoveryScanner.Result res =
        env.recoveryScanner().recover(tmp, WalRecoveryScanner.TailPolicy.SALVAGE_CONTINUE);
    // highest valid should be at least the second record
    assertThat(res.highestValidLsn).isGreaterThanOrEqualTo(2L);
  }

  private static WalRecord recordWithNMetrics(int n) {
    MetricEventBatch.Builder b = MetricEventBatch.newBuilder();
    for (int i = 0; i < n; i++) {
      b.addEvents(MetricEvent.newBuilder().setName("m" + i).addTs(1).addVals(1.0f).build());
    }
    return WalRecord.newBuilder().setEvent(b.build()).build();
  }
}
