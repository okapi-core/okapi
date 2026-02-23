package org.okapi.io;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.primitives.Ints;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.okapi.ds.ArraySlices;

public class OkapiCheckedCountingWriterTests {

  public static Stream<Arguments> testByteArrrayWritingTestCases() {
    return Stream.of(
        Arguments.of((Object) new byte[] {}),
        Arguments.of((Object) new byte[] {0x0}),
        Arguments.of((Object) new byte[] {0x0, 0x2}),
        Arguments.of((Object) new byte[] {0x0, 0x0, 0x3}));
  }

  @ParameterizedTest
  @MethodSource("testByteArrrayWritingTestCases")
  void testByteArrayWriting(byte[] buffer) throws IOException {
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeBytesWithoutLenPrefix(buffer);
    assertEquals(buffer.length, writer.getTotalBytesWritten());
    writer.writeChecksum();
    assertEquals(buffer.length + 4, writer.getTotalBytesWritten());

    var checkSum = getExpectedCheckSum(buffer);
    var output = os.toByteArray();
    assertArrayEquals(buffer, ArraySlices.slice(output, 0, buffer.length));
    assertEquals(checkSum, ArraySlices.readInt(output, output.length - 4));
  }

  @ParameterizedTest
  @MethodSource("testByteArrrayWritingTestCases")
  void testByteArrayWritingLenPrefix(byte[] buffer) throws IOException {
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeBytesWithLenPrefix(buffer);
    assertEquals(buffer.length + 4, writer.getTotalBytesWritten());
    writer.writeChecksum();
    assertEquals(buffer.length + 8, writer.getTotalBytesWritten());

    var checkSum = getExpectedCheckSum(Ints.toByteArray(buffer.length), buffer);
    var output = os.toByteArray();
    assertArrayEquals(buffer, ArraySlices.slice(output, 4, buffer.length));
    assertEquals(checkSum, ArraySlices.readInt(output, output.length - 4));
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 100, 10, 234})
  void testWriteInt(int x) throws IOException {
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeInt(x);
    assertEquals(4, writer.getTotalBytesWritten());
    writer.writeChecksum();
    assertEquals(8, writer.getTotalBytesWritten());
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, Long.MIN_VALUE, Long.MAX_VALUE, 0L, 100L, 10L, 234L})
  void testWriteLong(long x) throws IOException {
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeLong(x);
    assertEquals(8, writer.getTotalBytesWritten());
    writer.writeChecksum();
    assertEquals(12, writer.getTotalBytesWritten());
  }

  private int getExpectedCheckSum(byte[] bytes) {
    var crc = new CRC32();
    crc.update(bytes);
    return (int) crc.getValue();
  }

  private int getExpectedCheckSum(byte[] bytes, byte[] bytes2) {
    var crc = new CRC32();
    crc.update(bytes);
    crc.update(bytes2);
    return (int) crc.getValue();
  }
}
