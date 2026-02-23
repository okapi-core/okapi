package org.okapi.s3;

public interface ByteRangeSupplier {

  byte[] getBytes(long start, int len);

  long getEnd();
}
