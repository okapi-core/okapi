package org.okapi.metrics.storage;

import lombok.AllArgsConstructor;
import org.okapi.metrics.annotations.NotThreadSafe;

@NotThreadSafe
@AllArgsConstructor
public class BitValueReader implements ValueReader {
  BitReader bitReader;

  @Override
  public int readInteger(int bits) {
    int r = 0;
    var negative = bitReader.nextBit();
    for (int i = 1; i < bits; i++) {
      var nextBit = bitReader.nextBit();
      r = (r << 1) | (nextBit ? 1 : 0);
    }
    return (negative) ? (r == 0 ? (-(1 << (bits - 1))) : -r) : r;
  }

  @Override
  public int readUInt(int bits) {
    int r = 0;
    for (int i = 0; i < bits; i++) {
      var nextBit = bitReader.nextBit();
      r = (r << 1) | (nextBit ? 1 : 0);
    }
    return r;
  }

  @Override
  public boolean readBit() {
    return bitReader.nextBit();
  }
}
