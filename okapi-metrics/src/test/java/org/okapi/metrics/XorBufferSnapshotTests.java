/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.okapi.collections.OkapiLists;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.storage.ByteBufferWriter;
import org.okapi.metrics.storage.buffers.AppendOnlyByteBuffer;
import org.okapi.metrics.storage.buffers.BufferFullException;
import org.okapi.metrics.storage.buffers.HeapBufferAllocator;
import org.okapi.metrics.storage.xor.XorBuffer;
import org.okapi.testutils.OkapiTestUtils;

public class XorBufferSnapshotTests {

  private static XorBuffer checkpointAndRestore(
      XorBuffer ref, AppendOnlyByteBuffer appendOnlyByteBuffer)
      throws IOException, StreamReadingException {
    var snapshot = ref.snapshot();
    var tempFile = Files.createTempFile("xorSnapshot", ".tmp");
    try (var fos = new FileOutputStream(tempFile.toFile())) {
      snapshot.write(fos);
    }
    try (var fis = new FileInputStream(tempFile.toFile())) {
      return XorBuffer.initialize(fis, appendOnlyByteBuffer);
    }
  }

  @Test
  public void testXorSnapshotRestore()
      throws BufferFullException, IOException, StreamReadingException {
    var allocator = new HeapBufferAllocator();
    ByteBufferWriter bufferWriter = new ByteBufferWriter(allocator.allocate(20));
    var xorBuffer = new XorBuffer(bufferWriter);
    xorBuffer.push(0.01f);
    xorBuffer.push(0.02f);

    var restoredBuffer = checkpointAndRestore(xorBuffer, allocator.allocate(20));
    var restoredList = OkapiLists.toList(restoredBuffer.snapshot());
    assertEquals(Arrays.asList(0.01f, 0.02f), restoredList);

    restoredBuffer.push(0.03f);
    var afterWritingToRestored = OkapiLists.toList(restoredBuffer.snapshot());
    assertEquals(Arrays.asList(0.01f, 0.02f, 0.03f), afterWritingToRestored);
  }

  @Test
  public void testFuzzBuffer() throws BufferFullException, StreamReadingException, IOException {
    var L = OkapiTestUtils.genRandom(500.f, 10.f, 100);
    var capacity = 400;
    var allocator = new HeapBufferAllocator();
    ByteBufferWriter bufferWriter = new ByteBufferWriter(allocator.allocate(capacity));
    var ref = new XorBuffer(bufferWriter);
    for (var l : L) {
      ref.push(l);
    }

    var restored = checkpointAndRestore(ref, allocator.allocate(capacity));
    var randomEl = OkapiTestUtils.genSingle(500.f, 10.f);
    restored.push(randomEl);
    var restoredList = OkapiLists.toList(restored.snapshot());
    var newList =
        new ArrayList<>(L) {
          {
            add(randomEl);
          }
        };
    assertEquals(newList, restoredList);
  }
}
