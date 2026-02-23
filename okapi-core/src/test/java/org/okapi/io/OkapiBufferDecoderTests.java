package org.okapi.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class OkapiBufferDecoderTests {

  @Test
  void testSingleBuffer() throws IOException, NotEnoughBytesException {
    var decoder = new OkapiBufferDecoder();
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeInt(100);
    writer.writeLong(100L);
    writer.writeBytesWithLenPrefix(new byte[] {0x0});
    writer.writeBytesWithoutLenPrefix(new byte[] {0x1});
    writer.writeChecksum();
    var output = os.toByteArray();

    // check state management
    assertEquals(CheckedBufferDecoder.DECODER_STATE.CREATED, decoder.getState());
    decoder.setBuffer(output, 0, output.length);
    assertEquals(CheckedBufferDecoder.DECODER_STATE.INIT, decoder.getState());
    assertTrue(decoder.isCrcMatch());
    assertEquals(CheckedBufferDecoder.DECODER_STATE.VALIDATED, decoder.getState());

    // check reading sequence;
    assertEquals(100, decoder.nextInt());
    assertEquals(100L, decoder.nextLong());
    assertArrayEquals(new byte[] {0x0}, decoder.nextBytesLenPrefix());
    assertArrayEquals(new byte[] {0x1}, decoder.nextBytesNoLenPrefix(1));
  }

  @Test
  void testCorruptChecksum() throws IOException {
    var decoder = new OkapiBufferDecoder();
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeInt(100);
    writer.writeLong(100L);
    writer.writeBytesWithLenPrefix(new byte[] {0x0});
    writer.writeBytesWithoutLenPrefix(new byte[] {0x1});
    writer.writeChecksum();
    var output = os.toByteArray();
    output[output.length - 1] = 0x0;
    assertEquals(CheckedBufferDecoder.DECODER_STATE.CREATED, decoder.getState());
    decoder.setBuffer(output, 0, output.length);
    assertEquals(CheckedBufferDecoder.DECODER_STATE.INIT, decoder.getState());
    assertFalse(decoder.isCrcMatch());
    assertEquals(CheckedBufferDecoder.DECODER_STATE.INVALID_BUFFER, decoder.getState());
  }

  @Test
  public void testThrowsIfNotEnoughBytes() throws IOException {
    var decoder = new OkapiBufferDecoder();
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeInt(100);
    var output = os.toByteArray();
    decoder.setBuffer(output, 0, output.length);
    assertDoesNotThrow(decoder::nextInt);
    assertThrows(NotEnoughBytesException.class, decoder::nextLong);
  }

  @Test
  public void testThrowsIfNotEnoughBytes_withByteArrays() throws IOException {
    var decoder = new OkapiBufferDecoder();
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeBytesWithLenPrefix(new byte[] {0x0});
    var output = os.toByteArray();
    decoder.setBuffer(output, 0, output.length);
    assertDoesNotThrow(decoder::nextBytesLenPrefix);
    assertThrows(NotEnoughBytesException.class, decoder::nextBytesLenPrefix);
  }

  @Test
  public void testDecodingOffsetBuffer() throws IOException, NotEnoughBytesException {
    var decoder = new OkapiBufferDecoder();
    var os = new ByteArrayOutputStream();
    os.writeBytes(new byte[] {0x0, 0x1});
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeInt(100);
    writer.writeLong(100L);
    writer.writeBytesWithLenPrefix(new byte[] {0x0});
    writer.writeBytesWithoutLenPrefix(new byte[] {0x1});
    writer.writeChecksum();
    assertEquals(22, writer.getTotalBytesWritten());
    var output = os.toByteArray();

    // check state management
    assertEquals(CheckedBufferDecoder.DECODER_STATE.CREATED, decoder.getState());
    decoder.setBuffer(output, 2, output.length - 2);
    assertEquals(CheckedBufferDecoder.DECODER_STATE.INIT, decoder.getState());
    assertTrue(decoder.isCrcMatch());
    assertEquals(CheckedBufferDecoder.DECODER_STATE.VALIDATED, decoder.getState());

    // check reading sequence;
    assertEquals(100, decoder.nextInt());
    assertEquals(100L, decoder.nextLong());
    assertArrayEquals(new byte[] {0x0}, decoder.nextBytesLenPrefix());
    assertArrayEquals(new byte[] {0x1}, decoder.nextBytesNoLenPrefix(1));
  }

  @Test
  public void testDecodingOffsetBufferNoBoundaries() throws IOException, NotEnoughBytesException {
    var decoder = new OkapiBufferDecoder();
    var os = new ByteArrayOutputStream();
    var paddingLeft = new byte[] {0x0, 0x1, 0x3, 0x4, 0x5, 0x8};
    os.writeBytes(paddingLeft);
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeInt(100); // 4 bytes
    writer.writeLong(100L); // 8 bytes
    var toWrite = new byte[] {0x0, 0x1};
    writer.writeBytesWithLenPrefix(toWrite); // 104 bytes
    writer.writeBytesWithoutLenPrefix(new byte[] {0x3}); // 1bytes
    writer.writeChecksum(); // 4
    assertEquals(23, writer.getTotalBytesWritten());
    var paddingRight = new byte[] {0x2, 0x3};
    os.writeBytes(paddingRight);
    var output = os.toByteArray();

    // check state management
    assertEquals(CheckedBufferDecoder.DECODER_STATE.CREATED, decoder.getState());
    decoder.setBuffer(
        output, paddingLeft.length, output.length - paddingLeft.length - paddingRight.length);
    assertEquals(CheckedBufferDecoder.DECODER_STATE.INIT, decoder.getState());
    assertTrue(decoder.isCrcMatch());
    assertEquals(CheckedBufferDecoder.DECODER_STATE.VALIDATED, decoder.getState());

    // check reading sequence;
    assertEquals(100, decoder.nextInt());
    assertEquals(100L, decoder.nextLong());
    assertArrayEquals(toWrite, decoder.nextBytesLenPrefix());
    assertArrayEquals(new byte[] {0x3}, decoder.nextBytesNoLenPrefix(1));
  }
}
