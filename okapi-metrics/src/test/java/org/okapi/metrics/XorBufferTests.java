package org.okapi.metrics;

import static org.junit.jupiter.api.Assertions.*;

import org.okapi.collections.OkapiLists;
import org.okapi.metrics.storage.*;
import org.okapi.metrics.storage.buffers.BufferFullException;
import org.okapi.metrics.storage.buffers.DirectBufferAllocator;
import org.okapi.metrics.storage.fakes.SharedBufferAllocator;
import org.okapi.metrics.storage.xor.XorBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class XorBufferTests {

  @Test
  public void testWriteOneFloat() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var valueWriter = new ByteBufferWriter(allocator.allocate( 200));
    var xorBuffer = new XorBuffer(valueWriter);
    var val = 5000.f;
    xorBuffer.push(val);
    var reader = new BitValueReader(new ByteBufferReader(valueWriter.snapshot()));
    assertEquals(Float.floatToIntBits(5000.f), reader.readUInt(32));
  }

  @Test
  public void testWriteAFloatAndZero() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var valueWriter = new ByteBufferWriter(allocator.allocate( 200));
    var xorBuffer = new XorBuffer(valueWriter);
    var val = 5000.f;
    xorBuffer.push(val);
    xorBuffer.push(val);
    var reader = new BitValueReader(new ByteBufferReader(valueWriter.snapshot()));
    assertEquals(Float.floatToIntBits(5000.f), reader.readUInt(32));
    assertFalse(reader.readBit());
  }

  @ParameterizedTest
  @MethodSource("bufferSequences")
  public void testXorBufferSingle(List<Float> sequence) throws BufferFullException {
    var allocator = new DirectBufferAllocator();
    var valueWriter = new ByteBufferWriter(allocator.allocate( 200));
    var xorBuffer = new XorBuffer(valueWriter);
    for (var s : sequence) {
      xorBuffer.push(s);
    }
    var snapshot = xorBuffer.snapshot();
    var allDoubles = OkapiLists.toList(snapshot);
    assertEquals(sequence, allDoubles);
  }



  @Test
  public void testXorBufferTwoValues() throws BufferFullException {
    var sequence =
        new ArrayList<Float>() {
          {
            add(5000.05f);
            add(5000.06f);
          }
        };
    var allocator = new DirectBufferAllocator();
    var byteBufferWriter = new ByteBufferWriter(allocator.allocate(8000));
    var xorBuffer = new XorBuffer(byteBufferWriter);
    for (var val : sequence) {
      xorBuffer.push(val);
    }

    var output = OkapiLists.toList(xorBuffer.snapshot());
    assertEquals(sequence, output);
  }

  @Test
  public void testThreeValues() throws BufferFullException {
    var sequence =
        new ArrayList<Float>() {
          {
            add(5000.05f);
            add(5000.06f);
            add(5000.07f);
          }
        };
    var allocator = new DirectBufferAllocator();
    var byteBufferWriter = new ByteBufferWriter(allocator.allocate(8000));
    var xorBuffer = new XorBuffer(byteBufferWriter);
    for (var val : sequence) {
      xorBuffer.push(val);
    }

    var output = OkapiLists.toList(xorBuffer.snapshot());
    assertEquals(sequence, output);
  }

  @Test
  public void testInsert() throws BufferFullException {
    // either wrote more bits than expected or decoding failed
    var values = Arrays.asList(-499.65942f, -499.9441f, -499.84964f);
    var bufferWriter = new ByteBufferWriter(new SharedBufferAllocator().allocate(11));
    var buffer = new XorBuffer(bufferWriter);
    for(var v: values) {
      buffer.push(v);
    }
    var snapshot = buffer.snapshot();
    var allValues = OkapiLists.toList(snapshot);
    assertEquals(values, allValues);
  }

  public static Stream<Arguments> bufferSequences() {
    return Stream.of(
        Arguments.of(List.of(500.3f)),
        Arguments.of(List.of(-500.3f)),
        Arguments.of(Arrays.asList(500.3f, 500.3f)),
        Arguments.of(Arrays.asList(500.3f, 500.4f, 500.5f)),
        Arguments.of(Arrays.asList(-500.3f, -500.3f)),
        Arguments.of(Arrays.asList(Float.MIN_VALUE, Float.MIN_VALUE + 1)),
        Arguments.of(Arrays.asList(Float.MAX_VALUE, Float.MAX_VALUE - 1)),
        Arguments.of(Arrays.asList(Float.MAX_VALUE, Float.MIN_VALUE)),
        Arguments.of(Arrays.asList(-500.3f, -500.4f)),
        Arguments.of(Arrays.asList(-500.3f, -500.4f, -500.3f)),
        Arguments.of(Arrays.asList(-5000.3f, -5000.5f, -5000.5f)));
  }
}
