package org.okapi.io;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OkapiCheckedCountingWriter implements CheckedCountingWriter {

  OutputStream outputStream;
  int total = 0;
  CRC32 crc32;

  public OkapiCheckedCountingWriter(OutputStream os) {
    this.outputStream = os;
    this.crc32 = new CRC32();
  }

  @Override
  public int getTotalBytesWritten() {
    return total;
  }

  @Override
  public OutputStream getOutputStream() {
    return outputStream;
  }

  @Override
  public void writeInt(int x) throws IOException {
    writeBytesAndUpdateTotalAndChecksum(Ints.toByteArray(x));
  }

  @Override
  public void writeLong(long x) throws IOException {
    writeBytesAndUpdateTotalAndChecksum(Longs.toByteArray(x));
  }

  @Override
  public void writeBytesWithoutLenPrefix(byte[] bytes) throws IOException {
    writeBytesAndUpdateTotalAndChecksum(bytes);
  }

  @Override
  public void writeBytesWithLenPrefix(byte[] bytes) throws IOException {
    var len = bytes.length;
    writeBytesAndUpdateTotalAndChecksum(Ints.toByteArray(len));
    writeBytesAndUpdateTotalAndChecksum(bytes);
  }

  @Override
  public void writeChecksum() throws IOException {
    var checksum = (int) crc32.getValue();
    outputStream.write(Ints.toByteArray(checksum));
    total += 4;
  }

  @Override
  public void writeByte(byte b) throws IOException {
    outputStream.write(b);
    total += 1;
  }

  private void writeBytesAndUpdateTotalAndChecksum(byte[] bytes) throws IOException {
    outputStream.write(bytes);
    total += bytes.length;
    crc32.update(bytes);
  }

  @Override
  public int getChecksum() {
    return (int) crc32.getValue();
  }
}
