package org.okapi.primitives;

import org.okapi.io.StreamReadingException;

import java.io.IOException;

public interface RawSerializable {
  byte[] toByteArray() throws IOException;

  void fromByteArray(byte[] bytes, int offset, int len) throws StreamReadingException, IOException;

  int byteSize();
}
