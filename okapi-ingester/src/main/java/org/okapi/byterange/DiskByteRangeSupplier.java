package org.okapi.byterange;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import org.okapi.s3.ByteRangeSupplier;

public class DiskByteRangeSupplier implements ByteRangeSupplier, AutoCloseable {

  Path fp;
  MappedByteBuffer mappedByteBuffer;
  FileChannel channel;

  public DiskByteRangeSupplier(Path fp) throws IOException {
    this.fp = fp;
    this.channel = FileChannel.open(fp);
    this.mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, Files.size(fp));
  }

  @Override
  public byte[] getBytes(long start, int len) {
    var buffer = new byte[len];
    var oldPos = mappedByteBuffer.position();
    mappedByteBuffer.position((int) start);
    mappedByteBuffer.get(buffer, 0, len);
    mappedByteBuffer.position(oldPos);
    return buffer;
  }

  @Override
  public long getEnd() {
    try {
      return Files.size(fp);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws Exception {
    channel.close();
  }
}
