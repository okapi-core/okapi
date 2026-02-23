/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage.snapshots;

import java.io.IOException;
import java.io.OutputStream;
import lombok.AllArgsConstructor;
import org.okapi.io.OkapiIo;
import org.okapi.metrics.codec.GorillaCodec;
import org.okapi.metrics.storage.BitValueReader;
import org.okapi.metrics.storage.ByteBufferReader;
import org.okapi.metrics.storage.ValueReader;
import org.okapi.metrics.storage.buffers.BufferSnapshot;
import org.okapi.metrics.storage.timediff.GorillaBuffer;

@AllArgsConstructor
public class GorillaSnapshot implements org.okapi.metrics.storage.BufferSnapshot<Integer> {

  BufferSnapshot snapshot;
  ValueReader valueReader;
  int total;
  int served;

  public GorillaSnapshot(BufferSnapshot snapshot, int total) {
    this.snapshot = snapshot;
    this.total = total;
    this.served = 0;
    this.valueReader = new BitValueReader(new ByteBufferReader(snapshot));
  }

  @Override
  public boolean hasNext() {
    return served < total;
  }

  @Override
  public Integer next() {
    var value = GorillaCodec.readInteger(this.valueReader);
    served++;
    return value;
  }

  public void write(OutputStream os) throws IOException {
    OkapiIo.writeString(os, GorillaBuffer.MAGIC);
    OkapiIo.writeInt(os, total);
    snapshot.write(os);
    OkapiIo.writeString(os, GorillaBuffer.MAGIC_END);
  }

  public int size() {
    return total;
  }
}
