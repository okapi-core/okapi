package org.okapi.wal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.okapi.wal.Wal.MetricEvent;
import org.okapi.wal.Wal.WalRecord;

public class WalBatchWriterTest {

  private final ManualScheduler scheduler = new ManualScheduler();

  @AfterEach
  void tearDown() {
    scheduler.shutdownNow();
  }

  @Test
  void countBasedFlush_triggersAtMaxBatchSize() throws Exception {
    MetricWalRecordAdapter adapter = new MetricWalRecordAdapter();
    FakeWalWriter writer = new FakeWalWriter();
    WalFramer framer = new FakeFramer(20); // overhead not used directly here
    WalBatchWriter<MetricEvent> wbw =
        new WalBatchWriter<>(
            adapter,
            writer,
            framer,
            scheduler, /*wait*/
            Duration.of(0, ChronoUnit.SECONDS),
            /*maxBatchSize*/ 3, /*maxWalRecordSize*/
            10_000);

    var e1 = TestMetricBuilders.event("a", 1);
    var e2 = TestMetricBuilders.event("b", 1);
    var e3 = TestMetricBuilders.event("c", 1);

    wbw.consume(e1, e1.getSerializedSize());
    wbw.consume(e2, e2.getSerializedSize());
    wbw.consume(e3, e3.getSerializedSize()); // should flush

    assertThat(writer.records()).hasSize(1);
    WalRecord r = writer.records().get(0);
    assertThat(r.getEvent().getEventsCount()).isEqualTo(3);
  }

  @Test
  void sizeBasedFlush_respectsLimit() throws Exception {
    MetricWalRecordAdapter adapter = new MetricWalRecordAdapter();
    FakeWalWriter writer = new FakeWalWriter();
    WalFramer framer = new FakeFramer(24); // conservative overhead
    // tune limit so that 2 fit, adding 3rd exceeds
    WalBatchWriter<MetricEvent> wbw =
        new WalBatchWriter<>(
            adapter,
            writer,
            framer,
            scheduler,
            null,
            100, /*limit*/
            computeLimitForTwoEvents(adapter, framer));

    var e1 = TestMetricBuilders.event("x", 5);
    var e2 = TestMetricBuilders.event("y", 5);
    var e3 = TestMetricBuilders.event("z", 5);

    wbw.consume(e1, e1.getSerializedSize());
    wbw.consume(e2, e2.getSerializedSize());
    // Adding 3rd should flush first 2, then queue 3rd
    wbw.consume(e3, e3.getSerializedSize());

    assertThat(writer.records()).hasSize(1);
    assertThat(writer.records().get(0).getEvent().getEventsCount()).isEqualTo(2);

    wbw.flush(); // flush remaining 1
    assertThat(writer.records()).hasSize(2);
    assertThat(writer.records().get(1).getEvent().getEventsCount()).isEqualTo(1);
  }

  @Test
  void sizeBoundary_equalToLimit_isAccepted() throws Exception {
    MetricWalRecordAdapter adapter = new MetricWalRecordAdapter();
    FakeWalWriter writer = new FakeWalWriter();
    WalFramer framer = new FakeFramer(24);

    var e = TestMetricBuilders.event("fit", 10);
    WalRecord rec = adapter.buildRecord(List.of(e));
    int exact = framer.perRecordOverheadBytes() + 4 + rec.getSerializedSize();

    WalBatchWriter<MetricEvent> wbw =
        new WalBatchWriter<>(adapter, writer, framer, scheduler, null, 10, exact);

    wbw.consume(e, e.getSerializedSize());
    // no other trigger yet
    assertThat(writer.records()).isEmpty();

    wbw.flush();
    assertThat(writer.records()).hasSize(1);
  }

  @Test
  void hugeSingleEvent_throwsAndNoWrite() throws Exception {
    MetricWalRecordAdapter adapter = new MetricWalRecordAdapter();
    FakeWalWriter writer = new FakeWalWriter();
    WalFramer framer = new FakeFramer(24);

    var huge = TestMetricBuilders.event("huge", /*pairs*/ 2000); // intentionally big
    WalRecord rec = adapter.buildRecord(List.of(huge));
    int limit = framer.perRecordOverheadBytes() + 4 + rec.getSerializedSize() - 1;

    WalBatchWriter<MetricEvent> wbw =
        new WalBatchWriter<>(adapter, writer, framer, scheduler, null, 100, limit);

    assertThatThrownBy(() -> wbw.consume(huge, huge.getSerializedSize()))
        .isInstanceOf(org.okapi.wal.exceptions.VeryHugeRecordException.class);

    assertThat(writer.records()).isEmpty();
  }

  @Test
  void timeBasedFlush_triggersOnTimer() throws Exception {
    MetricWalRecordAdapter adapter = new MetricWalRecordAdapter();
    FakeWalWriter writer = new FakeWalWriter();
    WalFramer framer = new FakeFramer(24);

    WalBatchWriter<MetricEvent> wbw =
        new WalBatchWriter<>(
            adapter, writer, framer, scheduler, Duration.of(5, ChronoUnit.SECONDS), 100, 10_000);

    var e = TestMetricBuilders.event("t", 1);
    wbw.consume(e, e.getSerializedSize());

    // timer scheduled; trigger now
    scheduler.runNext();
    assertThat(writer.records()).hasSize(1);
  }

  @Test
  void noEmptyBatchOnTimer() throws Exception {
    MetricWalRecordAdapter adapter = new MetricWalRecordAdapter();
    FakeWalWriter writer = new FakeWalWriter();
    WalFramer framer = new FakeFramer(24);

    WalBatchWriter<MetricEvent> wbw =
        new WalBatchWriter<>(
            adapter, writer, framer, scheduler, Duration.of(5, ChronoUnit.SECONDS), 100, 10_000);

    // No events — directly trigger timer
    scheduler.runNext(); // no-op if none; ensure no writes
    assertThat(writer.records()).isEmpty();
  }

  @Test
  void closeFlushesRemaining() throws Exception {
    MetricWalRecordAdapter adapter = new MetricWalRecordAdapter();
    FakeWalWriter writer = new FakeWalWriter();
    WalFramer framer = new FakeFramer(24);

    WalBatchWriter<MetricEvent> wbw =
        new WalBatchWriter<>(adapter, writer, framer, scheduler, null, 100, 10_000);

    var e1 = TestMetricBuilders.event("a", 1);
    var e2 = TestMetricBuilders.event("b", 1);
    wbw.consume(e1, e1.getSerializedSize());
    wbw.consume(e2, e2.getSerializedSize());

    wbw.close();
    assertThat(writer.records()).hasSize(1);
    assertThat(writer.records().get(0).getEvent().getEventsCount()).isEqualTo(2);
  }

  private static int computeLimitForTwoEvents(MetricWalRecordAdapter adapter, WalFramer framer) {
    var e1 = TestMetricBuilders.event("s1", 5);
    var e2 = TestMetricBuilders.event("s2", 5);
    var two = adapter.buildRecord(List.of(e1, e2));
    return framer.perRecordOverheadBytes() + 4 + two.getSerializedSize();
  }
}
