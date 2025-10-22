package org.okapi.logs.io;

import com.google.common.primitives.Ints;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import org.okapi.logs.config.LogsConfigProperties;
import org.okapi.logs.index.PageIndex;
import org.okapi.logs.index.PageIndexEntry;

public class LogFileWriter {
  private final LogsConfigProperties cfg;

  public LogFileWriter(LogsConfigProperties cfg) {
    this.cfg = Objects.requireNonNull(cfg);
  }

  public Path partitionDir(String tenantId, String logStream) {
    return Path.of(cfg.getDataDir(), tenantId, logStream);
  }

  public Path partitionDir(String tenantId, String logStream, long tsMillis) {
    var hour = tsMillis / 3600_000L;
    return Path.of(cfg.getDataDir(), tenantId, logStream, Long.toString(hour));
  }

  public synchronized PageIndexEntry appendPage(String tenantId, String logStream, LogPage page)
      throws IOException {
    byte[] bytes = LogPageSerializer.serialize(page);

    Path part = partitionDir(tenantId, logStream, page.getTsStart());
    Files.createDirectories(part);
    Path bin = part.resolve("logfile.bin");
    Path idx = part.resolve("logfile.idx");

    long offset;
    try (FileChannel ch =
        FileChannel.open(
            bin, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
      offset = ch.size();
      ch.write(ByteBuffer.wrap(bytes));
      if (cfg.isFsyncOnPageAppend()) ch.force(true);
    }

    // Extract crc from the last 4 bytes
    int crc = Ints.fromByteArray(slice(bytes, bytes.length - 4, 4));

    var entry =
        PageIndexEntry.builder()
            .offset(offset)
            .length(bytes.length)
            .tsStart(page.getTsStart())
            .tsEnd(page.getTsEnd())
            .docCount(page.getMaxDocId() + 1)
            .crc32(crc)
            .build();

    new PageIndex(idx)
        .append(entry, cfg.getIndexFsyncEveryPages() == 1 || cfg.isFsyncOnPageAppend());

    return entry;
  }

  public byte[] readRange(Path bin, long offset, int length) throws IOException {
    try (FileChannel ch = FileChannel.open(bin, StandardOpenOption.READ)) {
      ByteBuffer bb = ByteBuffer.allocate(length);
      ch.read(bb, offset);
      return bb.array();
    }
  }

  private static byte[] slice(byte[] a, int off, int len) {
    byte[] r = new byte[len];
    System.arraycopy(a, off, r, 0, len);
    return r;
  }
}
