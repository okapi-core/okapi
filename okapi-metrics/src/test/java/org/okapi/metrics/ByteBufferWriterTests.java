package org.okapi.metrics;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.storage.ByteBufferReader;
import org.okapi.metrics.storage.ByteBufferWriter;
import org.okapi.metrics.storage.buffers.BufferSnapshot;
import org.okapi.metrics.storage.buffers.HeapBufferAllocator;
import org.okapi.metrics.storage.fakes.SharedBufferAllocator;

public class ByteBufferWriterTests {

  @ParameterizedTest
  @MethodSource("booleanSeqs")
  public void testByteBuffer(List<Boolean> seq) {
    var directAllocator = new SharedBufferAllocator();
    var buffer = directAllocator.allocate(100);
    var bufferWriter = new ByteBufferWriter(buffer);
    for (var s : seq) {
      bufferWriter.writeBit(s);
    }

    var snapshot = bufferWriter.snapshot();
    var bufferReader = new ByteBufferReader(bufferWriter.snapshot());
    assertEquals(snapshot.pos(), seq.size() / 8);
    var readSeq = new ArrayList<Boolean>();
    for (var s : seq) {
      readSeq.add(bufferReader.nextBit());
    }
    assertEquals(seq, readSeq);
  }

  @Test
  public void testSnapshot_1Bit() {
    var allocator = new SharedBufferAllocator();
    var buffer = allocator.allocate(1);
    var bufWriter = new ByteBufferWriter(buffer);
    bufWriter.writeBit(true);
    var bufferReader = new ByteBufferReader(bufWriter.snapshot());
    assertTrue(bufferReader.nextBit());
  }

  @Test
  public void testBufferWriter_3Bits() {
    var allocator = new SharedBufferAllocator();
    var buffer = allocator.allocate(1);
    var bufWriter = new ByteBufferWriter(buffer);
    bufWriter.writeBit(true);
    bufWriter.writeBit(false);
    bufWriter.writeBit(true);
    var b = bufWriter.getB();
    assertTrue((b & (1 << 7)) > 0);
    assertTrue((b & (1 << 6)) == 0);
    assertTrue((b & (1 << 5)) > 0);
  }

  @Test
  public void testSingleOverflow() {
    var allocator = new SharedBufferAllocator();
    var buffer = allocator.allocate(1);
    var bufWriter = new ByteBufferWriter(buffer);
    bufWriter.writeBit(true);
    bufWriter.writeBit(false);
    bufWriter.writeBit(true);
    bufWriter.writeBit(true);

    bufWriter.writeBit(false);
    bufWriter.writeBit(true);
    bufWriter.writeBit(false);
    bufWriter.writeBit(false);

    assertEquals("00000000", ByteDecoder.decode(bufWriter.getB()));
    bufWriter.writeBit(true);
    assertEquals(-128, bufWriter.getB());
    var b = bufWriter.getB();
    assertTrue((b & (1 << 7)) > 0);
  }

  @ParameterizedTest
  @MethodSource("booleanSeqs")
  public void testCheckpointRestore(List<Boolean> seq) throws IOException, StreamReadingException {
    var allocator = new HeapBufferAllocator();
    var buffer = allocator.allocate(100);
    var bufferWriter = new ByteBufferWriter(buffer);
    for (var s : seq) {
      bufferWriter.writeBit(s);
    }
    var snapshot = bufferWriter.snapshot();
    // write out the snapshot to temporary file on disk
    // restore the snapshot
    // check that is matches the original sequence
    var tempFile = Files.createTempFile("bufferWriterTest", ".tmp");
    try (var outputStream = Files.newOutputStream(tempFile)) {
      snapshot.write(outputStream);
    }
    ByteBufferWriter restoredWriter = null;
    var resetBuffer = allocator.allocate(200);
    try (var is = Files.newInputStream(tempFile)) {
      restoredWriter = ByteBufferWriter.initialize(is, resetBuffer);
    }
    var restoredSnapshot = restoredWriter.snapshot();
    checkMatch(seq, restoredSnapshot);
  }

  @Test
  public void testCheckpointRestore_1Bit() throws IOException, StreamReadingException {
    var directAllocator = new HeapBufferAllocator();
    var buffer = directAllocator.allocate(100);
    var bufferWriter = new ByteBufferWriter(buffer);
    bufferWriter.writeBit(true);
    var snapshot = bufferWriter.snapshot();
    // write out the snapshot to temporary file on disk
    // restore the snapshot
    // check that is matches the original sequence
    var tempFile = Files.createTempFile("bufferWriterTest", ".tmp");
    try (var outputStream = Files.newOutputStream(tempFile)) {
      snapshot.write(outputStream);
    }
    ByteBufferWriter restoredWriter = null;
    try (var is = Files.newInputStream(tempFile)) {
      restoredWriter = ByteBufferWriter.initialize(is, directAllocator.allocate(200));
    }
    var restoredSnapshot = restoredWriter.snapshot();
    var expectedParital = (byte)((1 << 7));
    assertEquals(expectedParital, restoredSnapshot.partial());
    checkMatch(Arrays.asList(true), restoredSnapshot);
  }

  @Test
  public void testCheckpointRestore_TooSmallBuffer() throws IOException {
    var directAllocator = new HeapBufferAllocator();
    // this one passes because a byte is always allocated
    var bufferWriter = new ByteBufferWriter(directAllocator.allocate(2));
    bufferWriter.writeBit(true);
    bufferWriter.writeBit(false);
    bufferWriter.writeBit(true);
    bufferWriter.writeBit(false);
    bufferWriter.writeBit(true);
    bufferWriter.writeBit(false);
    bufferWriter.writeBit(true);
    bufferWriter.writeBit(false);
    var snapshot = bufferWriter.snapshot();
    // write out the snapshot to temporary file on disk
    // restore the snapshot
    var tempFile = Files.createTempFile("bufferWriterTest", ".tmp");
    try (var outputStream = Files.newOutputStream(tempFile)) {
      snapshot.write(outputStream);
    }
    ByteBufferWriter restoredWriter = null;
    try (var is = Files.newInputStream(tempFile)) {
      assertThrows(BufferOverflowException.class, () -> ByteBufferWriter.initialize(is, directAllocator.allocate(0)));
    }
  }

  private void checkMatch(List<Boolean> seq, BufferSnapshot snapshot) {
    var bufferReader = new ByteBufferReader(snapshot);
    assertEquals(snapshot.pos(), seq.size() / 8);
    var readSeq = new ArrayList<Boolean>();
    for (var s : seq) {
      readSeq.add(bufferReader.nextBit());
    }
    assertEquals(seq, readSeq);
  }

  private static Stream<Arguments> booleanSeqs() {
    return java.util.stream.Stream.of(
        Arguments.of(Arrays.asList(true)),
        Arguments.of(Arrays.asList(true, false)),
        Arguments.of(Arrays.asList(true, false, true)),
        Arguments.of(Arrays.asList(true, true, true, true, true, true, true, true, false)),
        Arguments.of(
            Arrays.asList(true, false, true, false, true, false, true, false, true, false)),
        Arguments.of(
            Arrays.asList(
                true, false, true, false, true, false, true, false, true, false, true, false, true,
                false, true, false, true, false, true, false)));
  }
}
