package org.okapi.metrics;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.storage.ByteBufferWriter;
import org.okapi.metrics.storage.TsStore;
import org.okapi.metrics.storage.buffers.BufferFullException;
import org.okapi.metrics.storage.buffers.DirectBufferAllocator;
import org.okapi.metrics.storage.timediff.TimeDiffBuffer;
import org.okapi.metrics.storage.xor.XorBuffer;
import org.okapi.testutils.OkapiTestUtils;

public class TsStoreTests {
  @Test
  public void testTsStoreSingleThread() {
    var allocator = new DirectBufferAllocator();
    // 1 MB buffers for all time series
    var tsStore = new TsStore(allocator, 1_000_000, 1_000_000);
    var reading = new ReadingGenerator(Duration.of(10, ChronoUnit.MILLIS), 240);
    reading.populateRandom(500.f, 540.f);
    var ts = reading.getTimestamps();
    var vals = reading.getValues();
    for (int i = 0; i < ts.size(); i++) {
      tsStore.write("SeriesA", ts.get(i), vals.get(i));
    }
  }

  @Test
  public void testShardWindow() {
    var date = LocalDateTime.of(2025, 6, 19, 18, 0).toInstant(ZoneOffset.UTC);
    var datePlus2 = date.plus(2, ChronoUnit.HOURS);
    var datePlus1 = date.plus(1, ChronoUnit.HOURS);
    assertNotEquals(
        TsStore.getShardId("SeriesA", date.toEpochMilli()),
        TsStore.getShardId("SeriesA", datePlus2.toEpochMilli()));
    assertEquals(
        TsStore.getShardId("SeriesA", date.toEpochMilli()),
        TsStore.getShardId("SeriesA", datePlus1.toEpochMilli()));
  }

  @Test
  public void testCheckpointRestoreWithOneShardId() throws StreamReadingException, IOException {
    var allocator = new DirectBufferAllocator();
    // todo: 1mb buffers
    var tsStore = new TsStore(allocator, 1_000_000, 1_000_000);
    var reading = new ReadingGenerator(Duration.of(10, ChronoUnit.MILLIS), 10);
    reading.populateRandom(500.f, 540.f);
    var ts = reading.getTimestamps();
    var vals = reading.getValues();
    for (int i = 0; i < ts.size(); i++) {
      tsStore.write("SeriesA", ts.get(i), vals.get(i));
    }

    var restored = checkpointAndRestore(tsStore);
    var restoredSnap = restored.snapshot("SeriesA").get();
    var restoredTimes = new long[restoredSnap.size()];
    var restoredVals = new float[restoredSnap.size()];
    restoredSnap.next(restoredTimes, restoredVals);
    assertEquals(ts, OkapiTestUtils.toList(restoredTimes));
    assertEquals(vals, OkapiTestUtils.toList(restoredVals));
  }

  @Test
  public void testCheckpointRestoreWithTwoShardIds() throws StreamReadingException, IOException {
    var allocator = new DirectBufferAllocator();
    var tsStore = new TsStore(allocator, 1_000_000, 5_000_000);
    // 100 * 60 * 240 -> 1.44 million held in this shard -> 100
    var reading1 = new ReadingGenerator(Duration.of(100, ChronoUnit.MILLIS), 60);
    reading1.populateRandom(500.f, 540.f);
    var ts1 = reading1.getTimestamps();
    var vals1 = reading1.getValues();
    for (int i = 0; i < ts1.size(); i++) {
      tsStore.write("SeriesA", ts1.get(i), vals1.get(i));
    }

    var reading2 = new ReadingGenerator(Duration.of(100, ChronoUnit.MILLIS), 10);
    reading2.populateRandom(500.f, 540.f);
    var ts2 = reading1.getTimestamps();
    var vals2 = reading1.getValues();
    for (int i = 0; i < ts2.size(); i++) {
      tsStore.write("SeriesB", ts2.get(i), vals2.get(i));
    }

    var restored = checkpointAndRestore(tsStore);
    var restoredSnapA = restored.snapshot("SeriesA").get();

    var restoredTimes1 = new long[restoredSnapA.size()];
    var restoredVals1 = new float[restoredSnapA.size()];
    restoredSnapA.next(restoredTimes1, restoredVals1);
    assertEquals(ts1, OkapiTestUtils.toList(restoredTimes1));
    assertEquals(vals1, OkapiTestUtils.toList(restoredVals1));

    var restoredSnapB = restored.snapshot("SeriesB").get();
    var restoredTimes2 = new long[restoredSnapB.size()];
    var restoredVals2 = new float[restoredSnapB.size()];
    restoredSnapB.next(restoredTimes2, restoredVals2);
    assertEquals(ts2, OkapiTestUtils.toList(restoredTimes2));
    assertEquals(vals2, OkapiTestUtils.toList(restoredVals2));
  }

  @Test
  public void testCheckpoint2HrsBuffer() throws BufferFullException, IOException {
    // 100 * 60 * 240 -> 1.44 million held in this shard -> 100
    var reading1 = new ReadingGenerator(Duration.of(10, ChronoUnit.MILLIS), 240);
    reading1.populateRandom(500.f, 540.f);

    var allocator = new DirectBufferAllocator();
    var bufferWriter = new ByteBufferWriter(allocator.allocate(7_000_000));
    var xorBuffer = new XorBuffer(bufferWriter);
    var expectedVals = reading1.getValues();
    var expectedTs = reading1.getTimestamps();
    for (var val : expectedVals) {
      xorBuffer.push(val);
    }

    var timeDiffBuffer = new TimeDiffBuffer(allocator.allocate(7_000_000));
    for (var ts : expectedTs) {
      timeDiffBuffer.push(ts);
    }
    var valTempFile = Files.newTemporaryFile();
    try (var fos = new FileOutputStream(valTempFile)) {
      var start = System.currentTimeMillis();
      xorBuffer.snapshot().write(fos);
      var end = System.currentTimeMillis();
      System.out.println("Checkpointing vals took:  " + (end - start));
    }

    var timeTempFile = Files.newTemporaryFile();
    try (var fos = new FileOutputStream(timeTempFile)) {
      var start = System.currentTimeMillis();
      timeDiffBuffer.snapshot().write(fos);
      var end = System.currentTimeMillis();
      System.out.println("Checkpointing time took:  " + (end - start));
    }
  }

  private TsStore checkpointAndRestore(TsStore store) throws IOException, StreamReadingException {
    var buffer = new DirectBufferAllocator();
    var filePath = Files.newTemporaryFile();
    var start = System.currentTimeMillis();
    store.checkpoint(filePath.toPath());
    var end = System.currentTimeMillis();
    System.out.println("Time taken in checkpointing.." + (end - start));
    System.out.println("File size: " + (0. + filePath.length() / (1024L * 1024L) + " MB"));
    return TsStore.restore(filePath.toPath(), buffer);
  }
}
