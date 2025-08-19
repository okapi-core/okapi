package org.okapi.metrics.storage;

import static org.okapi.metrics.io.OkapiIo.checkMagicNumber;

import org.okapi.metrics.annotations.NotThreadSafe;
import org.okapi.metrics.storage.buffers.AppendOnlyByteBuffer;
import org.okapi.metrics.storage.buffers.BufferSnapshot;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.metrics.io.StreamReadingException;
import java.io.IOException;
import java.io.InputStream;
import lombok.Getter;

@NotThreadSafe
// todo: refactor writer should have an allocated buffer
public class ByteBufferWriter implements BitWriter {
  // state variables
  int nbits;
  @Getter byte b;
  @Getter AppendOnlyByteBuffer buffer;

  public ByteBufferWriter(AppendOnlyByteBuffer buffer) {
    this.buffer = buffer;
    this.nbits = 7; // start with the last bit available
    this.b = (byte) 0; // start with an empty byte
  }

  private ByteBufferWriter(int nbits, byte b, AppendOnlyByteBuffer buffer) {
    this.nbits = nbits;
    this.b = b;
    this.buffer = buffer;
  }

  public static ByteBufferWriter initialize(InputStream is, AppendOnlyByteBuffer byteBuffer)
      throws IOException, StreamReadingException {
    // read magic number
    checkMagicNumber(is, BufferSnapshot.MAGIC_NUMBER);
    // start reading the buffer
    // read position of the buffer
    var read = 0;
    var pos = OkapiIo.readInt(is);

    // read pos bytes from the stream
    var block = new byte[1024];
    int i = 0;
    while (i < pos) {
      var maxBlockSize = Math.min(1024, pos - i);
      // should not always read 1024 bytes
      read = is.read(block, 0, maxBlockSize);
      if (read == -1) {
        throw new StreamReadingException("Not enough bytes to read the buffer");
      }
      for (int j = 0; j < read && i < pos; j++, i++) {
        byteBuffer.put(block[j]);
      }
    }

    // read the partial byte and the number of bits in it
    var partial = (byte) is.read();
    if (partial == -1) {
      throw new StreamReadingException("Not enough bytes to read the partial byte");
    }
    var partialBits = is.read();

    if (partialBits == -1) {
      throw new StreamReadingException("Not enough bytes to read the partial bits");
    }

    if (partialBits > 7) {
      throw new StreamReadingException(
          "Invalid number of bits in the partial byte: " + partialBits);
    }

    // read magic number end
    checkMagicNumber(is, BufferSnapshot.MAGIC_NUMBER_END);
    return new ByteBufferWriter(partialBits, partial, byteBuffer);
  }

  @Override
  public void writeBit(boolean v) {
    b = (byte) (b | ((v ? 1 : 0) << nbits));
    nbits--;

    if (nbits < 0) {
      flush();
    }
  }

  @Override
  public boolean canWrite(int bits) {
    return buffer.canWriteBits(bits);
  }

  public void flush() {
    buffer.put(b);
    nbits = 7;
    b = (byte) 0;
  }

  public BufferSnapshot snapshot() {
    // O(1) snapshot
    return new BufferSnapshot(buffer, buffer.pos(), b, nbits);
  }
}
