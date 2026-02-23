package org.okapi.metrics.storage.buffers;

import java.io.IOException;
import java.io.OutputStream;
import org.okapi.io.OkapiIo;

public record BufferSnapshot(

    /**
     * @param partialBits: Starting from MSB (8th bit), which position to read down to.
     */
    AppendOnlyByteBuffer appendOnlyByteBuffer, int pos, byte partial, int partialBits) {

  public static final String MAGIC_NUMBER = "OKAPI_BUF";
  public static final String MAGIC_NUMBER_END = "END_OKAPI_BUF";

  static void writeInBlocks(
      OutputStream os, AppendOnlyByteBuffer memoryBuffer, int pos, int blockSize)
      throws IOException {
    // read in chunks of `blockSize`,
    if (blockSize < 0) {
      throw new IllegalArgumentException("Block size must be a positive number, got: " + blockSize);
    }
    var writeBuffer = new byte[blockSize];
    var st = 0;
    while (st < pos) {
      int off = 0;
      for (off = 0; off < blockSize && st + off < pos; off++) {
        writeBuffer[off] = memoryBuffer.get(st + off);
      }
      if (off == blockSize) {
        os.write(writeBuffer);
      } else {
        os.write(writeBuffer, 0, off);
      }
      st += off;
    }
  }

  public void write(OutputStream os) throws IOException {
    OkapiIo.writeString(os, MAGIC_NUMBER);
    OkapiIo.writeInt(os, pos);
    writeInBlocks(os, appendOnlyByteBuffer, appendOnlyByteBuffer.pos(), 4096);
    // write the partial byte and the number of bits in it
    OkapiIo.write(os, partial);
    // partial bits always between 0 and 7, so we can safely cast to byte
    OkapiIo.write(os, (byte) partialBits);
    OkapiIo.writeString(os, MAGIC_NUMBER_END);
  }
}
