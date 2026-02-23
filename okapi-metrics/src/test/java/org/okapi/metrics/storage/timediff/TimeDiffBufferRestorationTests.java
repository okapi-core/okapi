/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage.timediff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.okapi.collections.OkapiLists;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.storage.buffers.BufferFullException;
import org.okapi.metrics.storage.buffers.DirectBufferAllocator;
import org.okapi.testutils.OkapiTestUtils;

public class TimeDiffBufferRestorationTests {

  @Test
  public void testTimeDiffBuffer() throws BufferFullException, IOException, StreamReadingException {
    var allocator = new DirectBufferAllocator();
    var timeDiffBuffer = new TimeDiffBuffer(allocator.allocate(200));
    var t0 = System.currentTimeMillis();
    var testCase =
        new ArrayList<Long>() {
          {
            add(t0);
            add(t0 + 10);
            add(t0 + 5);
            add(t0 + 34);
          }
        };
    for (var t : testCase) {
      timeDiffBuffer.push(t);
    }
    var file = Files.createTempFile("timeDiff", ".tmp");
    try (var fos = new FileOutputStream(file.toFile())) {
      timeDiffBuffer.snapshot().write(fos);
    }
    try (var fis = new FileInputStream(file.toFile())) {
      var restoredBuffer = TimeDiffBuffer.initialize(fis, allocator.allocate(200));
      var restoredList = OkapiLists.toList(restoredBuffer.snapshot());
      assertEquals(testCase, restoredList);
    }
  }

  @Test
  public void testFuzzTest() throws BufferFullException, IOException, StreamReadingException {
    var allocator = new DirectBufferAllocator();
    var timeDiffBuffer = new TimeDiffBuffer(allocator.allocate(350));
    var current = System.currentTimeMillis();
    var series = OkapiTestUtils.getTimes(current, -10, 20, 300);
    for (var t : series) {
      timeDiffBuffer.push(t);
    }
    var file = Files.createTempFile("timeDiff", ".tmp");
    try (var fos = new FileOutputStream(file.toFile())) {
      timeDiffBuffer.snapshot().write(fos);
    }
    try (var fis = new FileInputStream(file.toFile())) {
      var restoredBuffer = TimeDiffBuffer.initialize(fis, allocator.allocate(350));
      var restoredList = OkapiLists.toList(restoredBuffer.snapshot());
      assertEquals(series, restoredList);
    }
  }
}
