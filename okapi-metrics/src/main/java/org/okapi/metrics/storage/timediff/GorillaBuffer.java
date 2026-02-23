/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage.timediff;

import static org.okapi.io.OkapiIo.checkMagicNumber;

import java.io.IOException;
import java.io.InputStream;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.annotations.NotThreadSafe;
import org.okapi.metrics.codec.GorillaCodec;
import org.okapi.metrics.storage.BitValueWriter;
import org.okapi.metrics.storage.ByteBufferWriter;
import org.okapi.metrics.storage.buffers.AppendOnlyByteBuffer;
import org.okapi.metrics.storage.buffers.BufferFullException;
import org.okapi.metrics.storage.snapshots.GorillaSnapshot;

@NotThreadSafe
public class GorillaBuffer {

  public static final String MAGIC = "GorillaBuffer";
  public static final String MAGIC_END = "GorillaBufferEnd";
  ByteBufferWriter byteBufferWriter;
  BitValueWriter bitValueWriter;
  int total;

  public GorillaBuffer(ByteBufferWriter byteBufferWriter) {
    this.byteBufferWriter = byteBufferWriter;
    this.bitValueWriter = new BitValueWriter(this.byteBufferWriter);
  }

  public static GorillaBuffer fromSnapshot(
      InputStream is, AppendOnlyByteBuffer appendOnlyByteBuffer)
      throws StreamReadingException, IOException {
    // check the magic number
    // this is to ensure that the stream is a GorillaBuffer
    checkMagicNumber(is, GorillaBuffer.MAGIC);
    // read the total number of integers
    var total = OkapiIo.readInt(is);
    // read the buffer
    var byteBufferWriter = ByteBufferWriter.initialize(is, appendOnlyByteBuffer);
    var gorillaBuffer = new GorillaBuffer(byteBufferWriter);
    gorillaBuffer.setTotal(total);
    // read the end magic number
    checkMagicNumber(is, GorillaBuffer.MAGIC_END);
    return gorillaBuffer;
  }

  public boolean writeInteger(int X) throws BufferFullException {
    var writeSuccessful = GorillaCodec.writeInteger(X, this.bitValueWriter);
    total += 1;
    return writeSuccessful;
  }

  public GorillaSnapshot snapshot() {
    // O(1) snapshot
    return new GorillaSnapshot(this.byteBufferWriter.snapshot(), total);
  }

  private void setTotal(int total) {
    this.total = total;
  }
}
