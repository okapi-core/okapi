/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
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
import org.okapi.metrics.storage.*;
import org.okapi.metrics.storage.fakes.SharedBufferAllocator;

public class BitValueWriterTests {

  public static Stream<Arguments> intSequences() {
    return Stream.of(
        Arguments.of(Arrays.asList(new VarInt(-10, 5))),
        Arguments.of(Arrays.asList(new VarInt(10, 5))),
        Arguments.of(Arrays.asList(new VarInt(10, 5), new VarInt(33, 7))),
        Arguments.of(Arrays.asList(new VarInt(10, 5), new VarInt(33, 7), new VarInt(13, 5))),
        Arguments.of(
            Arrays.asList(new VarInt(Integer.MIN_VALUE, 32), new VarInt(Integer.MAX_VALUE, 32))),
        Arguments.of(
            Arrays.asList(
                new VarInt(Integer.MIN_VALUE, 32),
                new VarInt(Integer.MIN_VALUE + 1, 32),
                new VarInt(Integer.MIN_VALUE + 2, 32),
                new VarInt(Integer.MIN_VALUE + 3, 32),
                new VarInt(Integer.MIN_VALUE + 4, 32))),
        Arguments.of(
            Arrays.asList(
                new VarInt(10, 5),
                new VarInt(33, 7),
                new VarInt(13, 5),
                new VarInt(32, 7),
                new VarInt((1 << 31) | (1 << 30), 33))));
  }

  public static Stream<Arguments> unsignedInts() {
    return Stream.of(
        Arguments.of(Arrays.asList(new VarInt(10, 5))),
        Arguments.of(Arrays.asList(new VarInt(10, 5), new VarInt(33, 7))),
        Arguments.of(Arrays.asList(new VarInt(10, 5), new VarInt(33, 7), new VarInt(13, 5))),
        Arguments.of(
            Arrays.asList(
                new VarInt(10, 5),
                new VarInt(33, 7),
                new VarInt(13, 5),
                new VarInt(32, 7),
                new VarInt((1 << 31) | (1 << 30), 33))));
  }

  public static Stream<Arguments> longSequences() {
    return Stream.of(
        Arguments.of(Arrays.asList(new VarLong(-10L, 5))),
        Arguments.of(Arrays.asList(new VarLong(10L, 5))),
        Arguments.of(Arrays.asList(new VarLong(Long.MIN_VALUE, 64))),
        Arguments.of(
            Arrays.asList(new VarLong(Long.MIN_VALUE, 64), new VarLong(Long.MAX_VALUE, 64))),
        Arguments.of(Arrays.asList(new VarLong(10L, 5), new VarLong(33L, 7))));
  }

  @ParameterizedTest
  @MethodSource("intSequences")
  public void testIntegerSequence(List<VarInt> varInts) {
    var bufferAllocator = new SharedBufferAllocator();
    var buffer = bufferAllocator.allocate(20);
    var byteBuffer = new ByteBufferWriter(buffer);
    var bufferWriter = new BitValueWriter(byteBuffer);
    for (var i : varInts) {
      bufferWriter.writeInteger(i.x(), i.bits());
    }
    var bufferReader = new ByteBufferReader(byteBuffer.snapshot());
    var bitValueReader = new BitValueReader(bufferReader);
    var decompressed = new ArrayList<Integer>();
    for (var i : varInts) {
      decompressed.add(bitValueReader.readInteger(i.bits()));
    }

    var expected = varInts.stream().map(VarInt::x).toList();
    assertEquals(expected, decompressed);
  }

  @Test
  public void testWriter() {
    var cases = Arrays.asList(10, -10);
    var reps = Arrays.asList("01010", "11010");
    for (int i = 0; i < cases.size(); i++) {
      var stringBitWriter = new StringBitWriter();
      var bitValueWriter = new BitValueWriter(stringBitWriter);
      bitValueWriter.writeInteger(cases.get(i), 5);
      assertEquals(reps.get(i), stringBitWriter.toString());
    }
  }

  @Test
  public void testMultiple() {
    var sbWriter = new StringBitWriter();
    var bitValueWriter = new BitValueWriter(sbWriter);
    bitValueWriter.writeInteger(10, 5);
    bitValueWriter.writeInteger(-10, 5);
    assertEquals("01010" + "11010", sbWriter.toString());
  }

  @Test
  public void testStringBitWriter() {
    var stringBitWriter = new StringBitWriter();
    stringBitWriter.writeBit(true);
    stringBitWriter.writeBit(false);
    assertEquals("10", stringBitWriter.toString());
  }

  //  @ParameterizedTest
  //  @MethodSource("longSequences")
  //  public void testLongWriting(List<VarLong> longs) {
  //    var allocator = new SharedBufferAllocator();
  //    var bitWriter = new ByteBufferWriter(allocator);
  //    var bitValueWriter = new BitValueWriter(bitWriter);
  //
  //    for (var l : longs) {
  //      bitValueWriter.writeLong(l.x(), l.bits());
  //    }
  //
  //    var snapshot = bitWriter.snapshot();
  //    var reader = new BitValueReader(new ByteBufferReader(snapshot));
  //    for (int i = 0; i < longs.size(); i++) {
  //      var expected = longs.get(i);
  //      var val = reader.readLong(expected.bits());
  //      assertEquals(expected.x(), val);
  //    }
  //  }

  @ParameterizedTest
  @MethodSource("unsignedInts")
  public void testUIntWriting(List<VarInt> varInts) {
    var bufferAllocator = new SharedBufferAllocator();
    var bitWriter = new ByteBufferWriter(bufferAllocator.allocate(20));
    var bufferWriter = new BitValueWriter(bitWriter);
    for (var i : varInts) {
      bufferWriter.writeInteger(i.x(), i.bits());
    }
    var bufferReader = new ByteBufferReader(bitWriter.snapshot());
    var bitValueReader = new BitValueReader(bufferReader);
    var decompressed = new ArrayList<Integer>();
    for (var i : varInts) {
      decompressed.add(bitValueReader.readInteger(i.bits()));
    }

    var expected = varInts.stream().map(VarInt::x).toList();
    assertEquals(expected, decompressed);
  }

  record VarInt(int x, int bits) {}

  record VarLong(long x, int bits) {}
}
