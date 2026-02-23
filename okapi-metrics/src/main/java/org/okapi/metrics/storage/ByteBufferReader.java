/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage;

import org.okapi.metrics.annotations.NotThreadSafe;
import org.okapi.metrics.storage.buffers.BufferSnapshot;

@NotThreadSafe
public class ByteBufferReader implements BitReader {

  // The ByteBuffer to read from
  BufferSnapshot byteBuffer;
  int pos = 0;
  int nBits = 7;
  int partialPos = 7;
  byte b;

  public ByteBufferReader(BufferSnapshot bufferSnapshot) {
    this.byteBuffer = bufferSnapshot;
    b = bufferSnapshot.appendOnlyByteBuffer().get(pos);
  }

  public boolean readFromPartial() {
    // partial starts reading from the 8th bit (1 << 7) and down towards bit pointed by snapshot
    var bit = (byteBuffer.partial() & (1 << partialPos)) > 0;
    partialPos--;
    return bit;
  }

  @Override
  public boolean nextBit() {
    if (nBits < 0) {
      pos++;
      b = byteBuffer.appendOnlyByteBuffer().get(pos);
      nBits = 7;
    }
    if (pos == byteBuffer.pos()) {
      return readFromPartial();
    }
    var bit = (b & (1 << (nBits))) > 0;
    nBits--;
    return bit;
  }

  @Override
  public boolean hasNext() {
    return pos < this.byteBuffer.pos() && partialPos >= this.byteBuffer.partialBits();
  }
}
