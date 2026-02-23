package org.okapi.metrics.storage;

import lombok.Getter;

public class StringBitWriter implements BitWriter {
  @Getter StringBuilder sb = new StringBuilder();

  @Override
  public void writeBit(boolean bit) {
    if (bit) sb.append('1');
    else sb.append('0');
  }

  @Override
  public boolean canWrite(int bits) {
    return true;
  }

  @Override
  public String toString() {
    return sb.toString();
  }
}
