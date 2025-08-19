package org.okapi.metrics.scanning;

import lombok.AllArgsConstructor;

import java.io.IOException;

@AllArgsConstructor
public class ArrayBackedBrs implements ByteRangeScanner {
  byte[] bytes;

  @Override
  public long totalBytes() throws IOException {
    return bytes.length;
  }

  @Override
  public byte[] getRange(long off, int nb) {
    var buffer = new byte[nb];
    System.arraycopy(bytes, (int) off, buffer, 0, nb);
    return buffer;
  }
}
