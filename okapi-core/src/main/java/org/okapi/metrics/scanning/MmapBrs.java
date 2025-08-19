package org.okapi.metrics.scanning;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

public class MmapBrs implements ByteRangeScanner {
  ByteBuffer mmap;
  File file;

  public MmapBrs(File file) throws IOException {
    this.file = file;
    RandomAccessFile raf = new RandomAccessFile(file, "r");
    FileChannel channel = raf.getChannel();
    var size = Files.size(file.toPath());
    mmap = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
  }

  @Override
  public long totalBytes() throws IOException {
    return Files.size(file.toPath());
  }

  @Override
  public byte[] getRange(long off, int nb) {
    if (off > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Cannot read from offset greater than Integer range.");
    }

    var buffer = new byte[nb];
    mmap.get((int) off, buffer);
    return buffer;
  }
}
