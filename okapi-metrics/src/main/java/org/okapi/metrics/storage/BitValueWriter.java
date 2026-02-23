/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage;

import lombok.AllArgsConstructor;
import org.okapi.metrics.annotations.NotThreadSafe;

@NotThreadSafe
@AllArgsConstructor
public class BitValueWriter implements ValueWriter {
  BitWriter bitWriter;

  @Override
  public void writeInteger(int v, int nBits) {
    bitWriter.writeBit(v < 0);
    v = (v > 0) ? v : -v;
    for (int i = nBits - 2; i >= 0; i--) {
      bitWriter.writeBit((v & (1 << i)) > 0);
    }
  }

  @Override
  public void writeBit(boolean b) {
    bitWriter.writeBit(b);
  }

  @Override
  public void writeUInt(int v, int bits) {
    for (int i = bits - 1; i >= 0; i--) {
      bitWriter.writeBit((v & (1L << i)) > 0);
    }
  }

  @Override
  public boolean canWriteBits(int bits) {
    return bitWriter.canWrite(bits);
  }
}
