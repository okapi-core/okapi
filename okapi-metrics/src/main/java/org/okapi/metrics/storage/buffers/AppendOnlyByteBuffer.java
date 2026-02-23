package org.okapi.metrics.storage.buffers;

import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AppendOnlyByteBuffer {

  ByteBuffer byteBuffer;
  int capacity;

  public void put(byte b) {
    byteBuffer.put(b);
  }

  public byte get(int idx) {
    if (idx >= capacity) {
      throw new IndexOutOfBoundsException(
          "Index " + idx + " is out of bounds for buffer of capacity " + capacity);
    }
    return byteBuffer.get(idx);
  }

  public int pos() {
    return byteBuffer.position();
  }

  public boolean canWriteBits(int nbits) {
    return (capacity - byteBuffer.position() - 1) * 8 >= nbits;
  }

  public boolean canWriteBytes(int nbytes) {
    return byteBuffer.position() + nbytes <= capacity;
  }

  protected ByteBuffer getByteBuffer() {
    return this.byteBuffer;
  }
}
