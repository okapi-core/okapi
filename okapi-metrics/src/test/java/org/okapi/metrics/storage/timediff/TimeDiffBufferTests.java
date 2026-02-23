/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage.timediff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.okapi.collections.OkapiLists;
import org.okapi.metrics.storage.buffers.BufferFullException;
import org.okapi.metrics.storage.buffers.DirectBufferAllocator;
import org.okapi.metrics.storage.fakes.SharedBufferAllocator;

public class TimeDiffBufferTests {

  public static Stream<Arguments> timeSeries() {
    var t0 = System.currentTimeMillis();
    return Stream.of(
        Arguments.of(Arrays.asList(t0)),
        Arguments.of(Arrays.asList(t0, t0)),
        Arguments.of(Arrays.asList(t0, t0 + 10)),
        Arguments.of(Arrays.asList(t0, t0 + 10, t0 + 20)),
        Arguments.of(Arrays.asList(t0, t0 + 10, t0 + 20, t0 + 30)),
        Arguments.of(Arrays.asList(t0, t0 + 10, t0 + 20, t0 + Integer.MAX_VALUE)),
        Arguments.of(Arrays.asList(t0, t0 - 10, t0 + 20, t0 + 30)));
  }

  @Test
  public void testStoreSingle() throws BufferFullException {
    var allocator = new DirectBufferAllocator();
    var timeDiffBuffer = new TimeDiffBuffer(allocator.allocate(10));
    var series = Arrays.asList(System.currentTimeMillis());
    timeDiffBuffer.push(series.get(0));

    var stored = OkapiLists.toList(timeDiffBuffer.snapshot());
    assertEquals(series, stored);
  }

  @Test
  public void testStoreTwo() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var timeDiffBuffer = new TimeDiffBuffer(allocator.allocate(10));
    var t0 = System.currentTimeMillis();
    var series = Arrays.asList(t0, t0 + 10);
    for (var s : series) {
      timeDiffBuffer.push(s);
    }

    var stored = OkapiLists.toList(timeDiffBuffer.snapshot());
    assertEquals(series, stored);
  }

  @Test
  public void testStoreThree() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var timeDiffBuffer = new TimeDiffBuffer(allocator.allocate(10));
    var t0 = System.currentTimeMillis();
    var series = Arrays.asList(t0, t0 + 10, t0 + 20);
    for (var s : series) {
      timeDiffBuffer.push(s);
    }

    var stored = OkapiLists.toList(timeDiffBuffer.snapshot());
    assertEquals(series, stored);
  }

  @Test
  public void testWrite4() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var timeDiffBuffer = new TimeDiffBuffer(allocator.allocate(20));
    var t0 = System.currentTimeMillis();
    timeDiffBuffer.push(t0);
    timeDiffBuffer.push(t0 + 10);
    timeDiffBuffer.push(t0 + 20);
    timeDiffBuffer.push(t0 + 30);
    var gorillaBuffer = timeDiffBuffer.getGorillaBuffer().snapshot();
    assertEquals(2, gorillaBuffer.size());

    var diffOfDiffs = OkapiLists.toList(timeDiffBuffer.getGorillaBuffer().snapshot());
    assertEquals(Arrays.asList(0, 0), diffOfDiffs);
  }

  @ParameterizedTest
  @MethodSource("timeSeries")
  public void testStoreSeries(List<Long> series) throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var timeDiffBuffer = new TimeDiffBuffer(allocator.allocate(20));
    for (var s : series) {
      timeDiffBuffer.push(s);
    }

    var stored = OkapiLists.toList(timeDiffBuffer.snapshot());
    assertEquals(series, stored);
  }

  @Test
  public void testStoreSeriesFuzz() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var timeDiffBuffer = new TimeDiffBuffer(allocator.allocate(2500));
    var series = new ArrayList<Long>();
    var t0 = System.currentTimeMillis();
    var random = new Random();
    for (int i = 0; i < 2000; i++) {
      t0 += random.nextInt(-10, 30);
      series.add(t0);
    }
    for (var s : series) {
      timeDiffBuffer.push(s);
    }
    var stored = OkapiLists.toList(timeDiffBuffer.snapshot());
    assertEquals(series, stored);
  }
}
