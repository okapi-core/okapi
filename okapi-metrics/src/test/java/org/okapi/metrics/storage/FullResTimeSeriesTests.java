package org.okapi.metrics.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.storage.buffers.HeapBufferAllocator;
import org.okapi.metrics.storage.timediff.TimeDiffBufferSnapshot;
import org.okapi.metrics.storage.xor.XorBufferSnapshot;
import org.okapi.testutils.OkapiTestUtils;

public class FullResTimeSeriesTests {

  public static Stream<Arguments> fuzzArgs() {
    return Stream.of(
        Arguments.of(2, 500.f, 0.1f, 1),
        Arguments.of(4, 500.f, 0.1f, 3),
        Arguments.of(4, -500.f, 0.1f, 3),
        Arguments.of(20, 500.f, 0.1f, 11),
        Arguments.of(20, -500.f, 0.1f, 11),
        Arguments.of(20, -500.f, 1f, 19),
        Arguments.of(200, -100.f, 10f, 19));
  }

  @Test
  public void testStoreSingleValue() throws CouldNotWrite {
    var allocator = new HeapBufferAllocator();
    var timeSeries = new FullResTimeSeries("timestream", allocator, 10, 10);
    var t0 = System.currentTimeMillis();
    timeSeries.put(t0, 0.0f);

    var snap = timeSeries.snapshot();
    var vals = new float[100];
    var ts = new long[100];
    snap.next(ts, vals);
    assertEquals(t0, ts[0]);
    assertEquals(0.0, vals[0]);
  }

  @Test
  public void testTwoValues() throws CouldNotWrite {
    var allocator = new HeapBufferAllocator();
    var timeSeries = new FullResTimeSeries("timestream", allocator, 10, 10);
    var t0 = System.currentTimeMillis();
    timeSeries.put(t0, 5000.1f);
    timeSeries.put(t0 + 10, 5000.2f);
    var snap = timeSeries.snapshot();
    var vals = new float[100];
    var ts = new long[100];
    snap.next(ts, vals);
    assertEquals(t0, ts[0]);
    assertEquals(5000.1f, vals[0]);

    assertEquals(t0 + 10, ts[1]);
    assertEquals(5000.2f, vals[1]);
  }

  @Test
  public void testThreeValues() throws CouldNotWrite {
    var allocator = new HeapBufferAllocator();
    var timeSeries = new FullResTimeSeries("timestream", allocator, 10, 10);
    var t0 = System.currentTimeMillis();
    timeSeries.put(t0, 5000.1f);
    timeSeries.put(t0 + 10, 5000.2f);
    timeSeries.put(t0 + 30, 5000.3f);
    var snap = timeSeries.snapshot();
    var vals = new float[100];
    var ts = new long[100];
    snap.next(ts, vals);
    assertEquals(t0, ts[0]);
    assertEquals(5000.1f, vals[0]);

    assertEquals(t0 + 10, ts[1]);
    assertEquals(5000.2f, vals[1]);

    assertEquals(t0 + 30, ts[2]);
    assertEquals(5000.3f, vals[2]);
  }

  @Test
  public void testThreeNegativeValues() throws CouldNotWrite {
    var allocator = new HeapBufferAllocator();
    var timeSeries = new FullResTimeSeries("timestream", allocator, 10, 10);
    var t0 = System.currentTimeMillis();
    timeSeries.put(t0, -5000.1f);
    timeSeries.put(t0 + 10, -5000.2f);
    timeSeries.put(t0 + 30, -5000.3f);
    var snap = timeSeries.snapshot();
    var vals = new float[100];
    var ts = new long[100];
    snap.next(ts, vals);
    assertEquals(t0, ts[0]);
    assertEquals(-5000.1f, vals[0]);

    assertEquals(t0 + 10, ts[1]);
    assertEquals(-5000.2f, vals[1]);

    assertEquals(t0 + 30, ts[2]);
    assertEquals(-5000.3f, vals[2]);
  }

  @ParameterizedTest
  @MethodSource("fuzzArgs")
  public void testSeriesFuzz(int cap, float base, float scale, int batchSize) throws CouldNotWrite {
    var allocator = new HeapBufferAllocator();
    var timeSeries = new FullResTimeSeries("timestream", allocator, 10, 10);
    var t = System.currentTimeMillis();
    var random = new Random();
    var expectedTs = new ArrayList<Long>();
    var expectedVals = new ArrayList<Float>();
    for (int i = 0; i < cap; i++) {
      t += random.nextInt(-10, 20);
      var value = base + scale * random.nextFloat();
      timeSeries.put(t, value);
      expectedTs.add(t);
      expectedVals.add(value);
    }

    var val = new float[batchSize];
    var ts = new long[batchSize];
    int checked = 0;
    var snapshot1 = timeSeries.snapshot();
    checkTimestamps(snapshot1.getTimeDiffBuffers(), expectedTs);
    checkValues(snapshot1.getXors(), expectedVals);

    // decode in batches
    var snap = timeSeries.snapshot();
    while (checked < snap.size()) {
      try {
        var decoded = snap.next(ts, val);
        if (decoded == 0 && checked < snap.size()) {
          System.out.println("Failed for test-case: ");
          System.out.println(expectedTs);
          System.out.println(expectedVals);
          assertTrue(false);
        }
        // check each of the decoded values
        for (int i = 0; i < decoded && checked < snap.size(); i++, checked++) {
          assertEquals(expectedVals.get(checked), val[i], "failed verifying value: " + i);
          assertEquals(expectedTs.get(checked), ts[i], "failed verifying timestamp: " + i);
        }

      } catch (Exception e) {
        System.out.println("Checked.." + checked);
        System.out.println("failed with exception: " + e.getMessage());
        System.out.println("Test case: " + expectedTs);
        System.out.println("Values: " + expectedVals);
        throw e;
      }
    }

    assertEquals(cap, checked);
  }

  @Test
  public void testSingleRestore() throws CouldNotWrite, StreamReadingException, IOException {
    var allocator = new HeapBufferAllocator();
    var timeSeries = new FullResTimeSeries("timestream", allocator, 10, 10);
    var t = System.currentTimeMillis();
    var expectedTs = OkapiTestUtils.getTimes(t, -10, 20, 4);
    var expectedVals = OkapiTestUtils.genRandom(500.f, 0.1f, 4);
    for (int i = 0; i < 4; i++) {
      timeSeries.put(expectedTs.get(i), expectedVals.get(i));
    }
    var restored = checkpointAndRestore(timeSeries, allocator);
    assertEquals(4, restored.total);
    assertEquals(1, restored.timestamps.size());

    var timebuffer = restored.timestamps.get(0);
    assertEquals(expectedTs.get(0), timebuffer.getFirst());
    assertEquals(expectedTs.get(1), timebuffer.getSecond());
    assertEquals(expectedTs.get(2), timebuffer.getBeforePrevious());
    assertEquals(expectedTs.get(3), timebuffer.getPrevious());
  }

  @ParameterizedTest
  @MethodSource("fuzzArgs")
  public void testCheckpointRestore(int cap, float base, float scale, int batchSize)
      throws CouldNotWrite, IOException, StreamReadingException {
    var allocator = new HeapBufferAllocator();
    var timeSeries = new FullResTimeSeries("timestream", allocator, 10, 10);
    var t = System.currentTimeMillis();
    var expectedTs = OkapiTestUtils.getTimes(t, -10, 20, cap);
    var expectedVals = OkapiTestUtils.genRandom(base, scale, cap);
    for (int i = 0; i < cap; i++) {
      timeSeries.put(expectedTs.get(i), expectedVals.get(i));
    }

    var restored = checkpointAndRestore(timeSeries, allocator);
    assertEquals(restored.total, cap);

    // generate another random value and append
    var random = new Random();
    var randomTime = expectedTs.getLast() + random.nextInt(-10, 20);
    var randomValue = base + scale * random.nextFloat();

    // write one more value and then recheck to see that all state variables are setup correctly
    restored.put(randomTime, randomValue);
    var restoredSnapshot = restored.snapshot();
    var vals = new float[cap + 1];
    var ts = new long[cap + 1];
    restoredSnapshot.next(ts, vals);
    assertEquals(OkapiTestUtils.toList(ts), OkapiTestUtils.copyAndAppend(expectedTs, randomTime));
    assertEquals(
        OkapiTestUtils.toList(vals), OkapiTestUtils.copyAndAppend(expectedVals, randomValue));
  }

  @Test
  public void testFuzzCase1() throws CouldNotWrite {
    var ts =
        Arrays.<Long>asList(
            1750492242474L,
            1750492242479L,
            1750492242498L,
            1750492242508L,
            1750492242508L,
            1750492242521L,
            1750492242530L,
            1750492242525L,
            1750492242534L,
            1750492242535L,
            1750492242528L,
            1750492242533L,
            1750492242537L,
            1750492242541L,
            1750492242532L,
            1750492242546L,
            1750492242537L,
            1750492242533L,
            1750492242533L,
            1750492242552L);
    var vals =
        Arrays.asList(
            -499.00156f,
            -499.46527f,
            -499.46188f,
            -499.91153f,
            -499.56055f,
            -499.50958f,
            -499.1062f,
            -499.29022f,
            -499.39072f,
            -499.87415f,
            -499.22345f,
            -499.1035f,
            -499.77634f,
            -499.9502f,
            -499.69608f,
            -499.40274f,
            -499.99316f,
            -499.71353f,
            -499.20316f,
            -499.38306f);
    var allocator = new HeapBufferAllocator();
    // issue is with spillover
    var timeSeries = new FullResTimeSeries("streamId", allocator, 8, 8);
    for (int i = 0; i < ts.size(); i++) {
      timeSeries.put(ts.get(i), vals.get(i));
    }

    var storedTs = new long[19];
    var storedVals = new float[19];

    var decoded = timeSeries.snapshot().next(storedTs, storedVals);
    assertEquals(19, decoded);
    for (int i = 0; i < 19; i++) {
      assertEquals(storedTs[i], ts.get(i));
      assertEquals(storedVals[i], vals.get(i));
    }
  }

  @Test
  public void testBufferSwitch() throws CouldNotWrite {
    var allocator = new HeapBufferAllocator();
    var timeSeries = new FullResTimeSeries("timestream", allocator, 8, 5);
    var time = System.currentTimeMillis();
    timeSeries.put(time, 0.1f);
    timeSeries.put(time + 20, 0.1f);
    var tbuf = new long[2];
    var val = new float[2];
    timeSeries.snapshot().next(tbuf, val);
    assertEquals(time, tbuf[0]);
    assertEquals(time + 20, tbuf[1]);
  }

  private FullResTimeSeries checkpointAndRestore(FullResTimeSeries ref, BufferAllocator allocator)
      throws IOException, StreamReadingException {
    var file = Files.createTempFile("checkpoint", ".file");
    try (var fos = new FileOutputStream(file.toFile())) {
      ref.snapshot().write(fos);
    }
    try (var is = new FileInputStream(file.toFile())) {
      return FullResTimeSeries.restore(is, allocator);
    }
  }

  public void checkValues(List<XorBufferSnapshot> xors, List<Float> expectedValues) {
    var allInBuffer = new ArrayList<Float>();
    for (int i = 0; i < xors.size(); i++) {
      var xor = xors.get(i);
      while (xor.hasNext()) {
        allInBuffer.add(xor.next());
      }
    }
    assertEquals(expectedValues, allInBuffer, "Values in buffer do not match expected values");
  }

  public void checkTimestamps(
      List<TimeDiffBufferSnapshot> timeDiffBufferSnapshots, List<Long> expectedTs) {
    var ts = new ArrayList<Long>();
    for (var timeDiffBufferSnapshot : timeDiffBufferSnapshots) {
      while (timeDiffBufferSnapshot.hasNext()) {
        ts.add(timeDiffBufferSnapshot.next());
      }
    }
    assertEquals(expectedTs, ts, "Timestamps in buffer do not match expected timestamps");
  }
}
