package org.okapi.metrics.storage;

public interface ValueWriter {
  void writeInteger(int X, int bits);

  void writeBit(boolean b);

  void writeUInt(int X, int bits);

  //    void writeLong(long X, int bits);
  boolean canWriteBits(int bits);
}
