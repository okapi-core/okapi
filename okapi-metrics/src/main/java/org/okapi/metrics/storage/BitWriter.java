package org.okapi.metrics.storage;

public interface BitWriter {
  void writeBit(boolean bit);

  boolean canWrite(int bits);
}
