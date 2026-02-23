package org.okapi.io;

import java.io.IOException;
import java.io.OutputStream;

public interface CheckedCountingWriter {
  int getTotalBytesWritten();

  OutputStream getOutputStream();

  void writeInt(int x) throws IOException;

  void writeLong(long x) throws IOException;

  void writeBytesWithoutLenPrefix(byte[] bytes) throws IOException;

  void writeBytesWithLenPrefix(byte[] bytes) throws IOException;

  void writeChecksum() throws IOException;

  void writeByte(byte b) throws IOException;

  int getChecksum();
}
