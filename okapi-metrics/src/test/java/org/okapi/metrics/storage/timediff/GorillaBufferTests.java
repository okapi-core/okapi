package org.okapi.metrics.storage.timediff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.okapi.collections.OkapiLists;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.storage.ByteBufferWriter;
import org.okapi.metrics.storage.buffers.BufferFullException;
import org.okapi.metrics.storage.buffers.HeapBufferAllocator;
import org.okapi.metrics.storage.fakes.SharedBufferAllocator;
import org.okapi.metrics.storage.snapshots.GorillaSnapshot;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class GorillaBufferTests {

  @Test
  public void testSingleThreaded() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var bufferWriter = new ByteBufferWriter(allocator.allocate(16));
    var gorillaBuffer = new GorillaBuffer(bufferWriter);
    gorillaBuffer.writeInteger(10);
    gorillaBuffer.writeInteger(40);
    gorillaBuffer.writeInteger(Integer.MAX_VALUE);
    gorillaBuffer.writeInteger(Integer.MIN_VALUE);

    var gorillaSnapshot = gorillaBuffer.snapshot();
    var allInBuffer = new ArrayList<Integer>();
    gorillaSnapshot.forEachRemaining(allInBuffer::add);

    assertEquals(Arrays.asList(10, 40, Integer.MAX_VALUE, Integer.MIN_VALUE), allInBuffer);
  }

  @Test
  public void testZeros() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var bufferWriter = new ByteBufferWriter(allocator.allocate(10));
    var gorillaBuffer = new GorillaBuffer(bufferWriter);
    gorillaBuffer.writeInteger(0);
    gorillaBuffer.writeInteger(0);
    gorillaBuffer.writeInteger(Integer.MAX_VALUE);
    gorillaBuffer.writeInteger(Integer.MIN_VALUE);

    var gorillaSnapshot = gorillaBuffer.snapshot();
    var allInBuffer = new ArrayList<Integer>();
    gorillaSnapshot.forEachRemaining(allInBuffer::add);

    assertEquals(Arrays.asList(0, 0, Integer.MAX_VALUE, Integer.MIN_VALUE), allInBuffer);
  }

  @Test
  public void testSnapshotImmutability() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var bufferWriter = new ByteBufferWriter(allocator.allocate(10));
    var gorillaBuffer = new GorillaBuffer(bufferWriter);
    gorillaBuffer.writeInteger(10);
    var snap1 = gorillaBuffer.snapshot();

    gorillaBuffer.writeInteger(20);
    var snap2 = gorillaBuffer.snapshot();
    assertEquals(Arrays.asList(10), OkapiLists.toList(snap1));
    assertEquals(Arrays.asList(10, 20), OkapiLists.toList(snap2));
  }

  @Test
  public void testSnapshotLargeBuffer() throws BufferFullException {
    var allocator = new SharedBufferAllocator();
    var bufferWriter = new ByteBufferWriter(allocator.allocate(2000));
    var gorillaBuffer = new GorillaBuffer(bufferWriter);
    var expected = new ArrayList<Integer>();
    GorillaSnapshot snapAt100 = null;
    for (int i = 0; i < 500; i++) {
      gorillaBuffer.writeInteger(i);
      if (i == 100) {
        snapAt100 = gorillaBuffer.snapshot();
      }
      expected.add(i);
    }

    var snap = gorillaBuffer.snapshot();
    assertEquals(expected, OkapiLists.toList(snap));
    assertEquals(expected.subList(0, 101), OkapiLists.toList(snapAt100));
  }


  @Test
  public void testSnapshotImmutability_fuzzy() throws BufferFullException {
    var allocator = new HeapBufferAllocator();
    var bufferWriter = new ByteBufferWriter(allocator.allocate(4500));
    var gorillaBuffer = new GorillaBuffer(bufferWriter);
    var testCase = new ArrayList<Integer>();
    var fuzzLimit = 900;
    var random = new Random();
    for (int i = 0; i < fuzzLimit; i++) {
      var randomInteger = random.nextInt(Integer.MAX_VALUE);
      testCase.add(randomInteger);
    }

    var snapshots = new ArrayList<GorillaSnapshot>();
    for (int i = 0; i < fuzzLimit; i++) {
      gorillaBuffer.writeInteger(testCase.get(i));
      snapshots.add(gorillaBuffer.snapshot());
    }

    for (int bound = 1; bound < fuzzLimit; bound++) {
      assertEquals(testCase.subList(0, bound), OkapiLists.toList(snapshots.get(bound - 1)), String.format("Fuzz failed for bound: %s", bound));
    }
  }

  @Test
  public void testCheckpointRestore() throws BufferFullException, IOException, StreamReadingException {
    var allocator = new HeapBufferAllocator();
    var bufferCapacity = 4500;
    var bufferWriter = new ByteBufferWriter(allocator.allocate(bufferCapacity));
    var gorillaBuffer = new GorillaBuffer(bufferWriter);
    var fuzzLimit = 900;
    var random = new Random();
    var testCase = new ArrayList<Integer>();
    for (int i = 0; i < fuzzLimit; i++) {
      var randomInteger = random.nextInt(Integer.MAX_VALUE);
      testCase.add(randomInteger);
      gorillaBuffer.writeInteger(randomInteger);
    }

    var snapshot = gorillaBuffer.snapshot();
    var tempFile = Files.createTempFile("gorilla", ".tmp");
    try (var fos = new FileOutputStream(tempFile.toFile())){
      snapshot.write(fos);
    }
    var secondBuffer = allocator.allocate(bufferCapacity);
    try (var is = new FileInputStream(tempFile.toFile())){
      var restored = GorillaBuffer.fromSnapshot(is, secondBuffer);
      var snapshotFromRestored = restored.snapshot();
      var allIntegers = OkapiLists.toList(snapshotFromRestored);
      assertEquals(testCase, allIntegers);
    } 
  }

}
