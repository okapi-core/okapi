package org.okapi.logs.index;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class PageIndex {
  public static final int ENTRY_BYTES = 8 + 4 + 8 + 8 + 4 + 4; // 36 bytes

  private final Path indexPath;

  public PageIndex(Path indexPath) {
    this.indexPath = indexPath;
  }

  public synchronized void append(PageIndexEntry e) throws IOException {
    Files.createDirectories(indexPath.getParent());
    try (FileChannel ch =
        FileChannel.open(
            indexPath,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND)) {
      ByteBuffer bb = ByteBuffer.allocate(ENTRY_BYTES);
      bb.put(Longs.toByteArray(e.getOffset()));
      bb.put(Ints.toByteArray(e.getLength()));
      bb.put(Longs.toByteArray(e.getTsStart()));
      bb.put(Longs.toByteArray(e.getTsEnd()));
      bb.put(Ints.toByteArray(e.getDocCount()));
      bb.put(Ints.toByteArray(e.getCrc32()));
      bb.flip();
      ch.write(bb);
    }
  }

  public List<PageIndexEntry> readAll() throws IOException {
    List<PageIndexEntry> out = new ArrayList<>();
    if (!Files.exists(indexPath)) return out;
    try (FileChannel ch = FileChannel.open(indexPath, StandardOpenOption.READ)) {
      long size = ch.size();
      long pos = 0;
      ByteBuffer bb = ByteBuffer.allocate(ENTRY_BYTES);
      while (pos + ENTRY_BYTES <= size) {
        bb.clear();
        ch.read(bb, pos);
        pos += ENTRY_BYTES;
        byte[] arr = bb.array();
        int p = 0;
        long offset = Longs.fromByteArray(slice(arr, p, 8));
        p += 8;
        int length = Ints.fromByteArray(slice(arr, p, 4));
        p += 4;
        long tsStart = Longs.fromByteArray(slice(arr, p, 8));
        p += 8;
        long tsEnd = Longs.fromByteArray(slice(arr, p, 8));
        p += 8;
        int docCount = Ints.fromByteArray(slice(arr, p, 4));
        p += 4;
        int crc = Ints.fromByteArray(slice(arr, p, 4));
        out.add(
            PageIndexEntry.builder()
                .offset(offset)
                .length(length)
                .tsStart(tsStart)
                .tsEnd(tsEnd)
                .docCount(docCount)
                .crc32(crc)
                .build());
      }
    }
    return out;
  }

  public List<PageIndexEntry> range(long startInclusive, long endInclusive) throws IOException {
    List<PageIndexEntry> all = readAll();
    List<PageIndexEntry> out = new ArrayList<>();
    for (PageIndexEntry e : all) {
      if (e.getTsEnd() < startInclusive || e.getTsStart() > endInclusive) continue;
      out.add(e);
    }
    return out;
  }

  private static byte[] slice(byte[] a, int off, int len) {
    byte[] r = new byte[len];
    System.arraycopy(a, off, r, 0, len);
    return r;
  }
}
