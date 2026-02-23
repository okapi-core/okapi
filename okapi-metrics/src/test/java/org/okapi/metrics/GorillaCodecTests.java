package org.okapi.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.okapi.metrics.codec.GorillaCodec;
import org.okapi.metrics.storage.*;
import org.okapi.metrics.storage.buffers.BufferFullException;
import org.okapi.metrics.storage.fakes.SharedBufferAllocator;

public class GorillaCodecTests {

  public static Stream<Arguments> boundaryValues() {
    return Stream.of(
        Arguments.of(Arrays.asList(0, 0, 0)),
        Arguments.of(Arrays.asList(1, 0, 1)),
        Arguments.of(Arrays.asList(10, 20)),
        Arguments.of(Arrays.asList(-10, 20)),
        Arguments.of(Arrays.asList(-10, 20)),
        Arguments.of(Arrays.asList(-2048, 2048)),
        Arguments.of(
            Arrays.asList(
                -64,
                64,
                -255,
                256,
                -256,
                -2048,
                2047,
                2048,
                Integer.MIN_VALUE + 1,
                Integer.MAX_VALUE)));
  }

  @Test
  public void testGorillaEncoding() throws BufferFullException {
    var bitWriter = new StringBitWriter();
    var valueWriter = new BitValueWriter(bitWriter);
    GorillaCodec.writeInteger(1, valueWriter);
    assertEquals("100000001", bitWriter.toString());
  }

  @Test
  public void testGorilla_Minus256() throws BufferFullException {
    var bitWriter = new StringBitWriter();
    var valueWriter = new BitValueWriter(bitWriter);
    GorillaCodec.writeInteger(-256, valueWriter);
    assertEquals("110 100 000 000".replaceAll(" ", ""), bitWriter.toString());
  }

  @Test
  public void testGorilla_256() throws BufferFullException {
    var bitWriter = new StringBitWriter();
    var valueWriter = new BitValueWriter(bitWriter);
    GorillaCodec.writeInteger(256, valueWriter);
    assertEquals("1110 0001 0000 0000".replaceAll(" ", ""), bitWriter.toString());
  }

  @Test
  public void testGorilla_M2048() throws BufferFullException {
    var bitWriter = new StringBitWriter();
    var valueWriter = new BitValueWriter(bitWriter);
    GorillaCodec.writeInteger(-2048, valueWriter);
    assertEquals("1110 1000 0000 0000".replaceAll(" ", ""), bitWriter.toString());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 1, -255, 256, -2048, 2048, Integer.MIN_VALUE, Integer.MAX_VALUE})
  public void testGorillaCodec(int value) throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var byteBufferedWriter = new ByteBufferWriter(allocator.allocate(12));
    var bitWriter = new BitValueWriter(byteBufferedWriter);
    GorillaCodec.writeInteger(value, bitWriter);

    var snapshotReader = new ByteBufferReader(byteBufferedWriter.snapshot());
    var bitValueReader = new BitValueReader(snapshotReader);
    var read = GorillaCodec.readInteger(bitValueReader);
    assertEquals(value, read);
  }

  @Test
  public void testSanityGorillaCodec() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var byteBufferedWriter = new ByteBufferWriter(allocator.allocate(10));
    var bitWriter = new BitValueWriter(byteBufferedWriter);
    GorillaCodec.writeInteger(10, bitWriter);
    GorillaCodec.writeInteger(20, bitWriter);

    var snapshotReader = new ByteBufferReader(byteBufferedWriter.snapshot());
    var bitValueReader = new BitValueReader(snapshotReader);
    var read1 = GorillaCodec.readInteger(bitValueReader);
    assertEquals(10, read1);

    var read2 = GorillaCodec.readInteger(bitValueReader);
    assertEquals(20, read2);
  }

  @ParameterizedTest
  @MethodSource("boundaryValues")
  public void testSequentialRead(List<Integer> seq) throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var byteBufferedWriter = new ByteBufferWriter(allocator.allocate(seq.size() * 4));
    var bitWriter = new BitValueWriter(byteBufferedWriter);

    for (var s : seq) {
      GorillaCodec.writeInteger(s, bitWriter);
    }

    var read = new ArrayList<Integer>();
    var snapshotReader = new ByteBufferReader(byteBufferedWriter.snapshot());
    var bitValueReader = new BitValueReader(snapshotReader);
    for (var s : seq) {
      read.add(GorillaCodec.readInteger(bitValueReader));
    }
    assertEquals(seq, read);
  }

  @Test
  public void testPartialWrites() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var byteBufferedWriter = new ByteBufferWriter(allocator.allocate(20));
    var bitWriter = new BitValueWriter(byteBufferedWriter);
    GorillaCodec.writeInteger(10, bitWriter);
    var read = new ArrayList<Integer>();
    var snapshotReader = new ByteBufferReader(byteBufferedWriter.snapshot());
    var bitValueReader = new BitValueReader(snapshotReader);
  }
}
