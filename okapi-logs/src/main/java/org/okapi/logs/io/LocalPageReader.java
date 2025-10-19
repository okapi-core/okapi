package org.okapi.logs.io;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.okapi.logs.stats.StatsEmitter;
import org.okapi.protos.logs.LogPayloadProto;
import org.roaringbitmap.RoaringBitmap;

public class LocalPageReader implements Closeable {
  private static final int HEADER_SIZE = 80;

  private final StatsEmitter stats;
  private final ChannelCache channelCache;

  public LocalPageReader(StatsEmitter stats, int maxOpenChannels) {
    this.stats = stats;
    this.channelCache = new ChannelCache(maxOpenChannels);
  }

  public Header readHeader(Path bin, long offset) throws IOException {
    byte[] hdr = range(bin, offset, HEADER_SIZE);
    int pos = 0;
    long tsStart = Longs.fromByteArray(slice(hdr, pos, 8));
    pos += 8;
    long tsEnd = Longs.fromByteArray(slice(hdr, pos, 8));
    pos += 8;
    int maxDocId = Ints.fromByteArray(slice(hdr, pos, 4));
    pos += 4;
    int lenTri = Ints.fromByteArray(slice(hdr, pos, 4));
    pos += 4;
    int lenLvl = Ints.fromByteArray(slice(hdr, pos, 4));
    pos += 4;
    int lenBloom = Ints.fromByteArray(slice(hdr, pos, 4));
    pos += 4;
    int lenDocs = Ints.fromByteArray(slice(hdr, pos, 4));
    return new Header(tsStart, tsEnd, maxDocId, lenTri, lenLvl, lenBloom, lenDocs);
  }

  public RoaringBitmap readLevelBitmap(Path bin, long pageOffset, Header h, int level)
      throws IOException {
    if (h.lenLvl() == 0) return null;
    long secOff = pageOffset + HEADER_SIZE + h.lenTri();
    byte[] sec = range(bin, secOff, h.lenLvl());
    int pos = 0;
    while (pos < sec.length) {
      int levelCode = Ints.fromByteArray(slice(sec, pos, 4));
      pos += 4;
      int n = Ints.fromByteArray(slice(sec, pos, 4));
      pos += 4;
      if (levelCode == level) {
        RoaringBitmap bm = new RoaringBitmap();
        try (java.io.DataInputStream dis =
            new java.io.DataInputStream(new java.io.ByteArrayInputStream(slice(sec, pos, n)))) {
          bm.deserialize(dis);
        }
        return bm;
      } else {
        pos += n;
      }
    }
    return null;
  }

  public RoaringBitmap readTrigramIntersection(Path bin, long pageOffset, Header h, int[] tris)
      throws IOException {
    if (h.lenTri() == 0 || tris == null || tris.length == 0) return null;
    long secOff = pageOffset + HEADER_SIZE;
    byte[] sec = range(bin, secOff, h.lenTri());
    java.util.Set<Integer> target = new java.util.HashSet<>();
    for (int t : tris) target.add(t);

    RoaringBitmap acc = null;
    int pos = 0;
    while (pos < sec.length) {
      int triIndex = Ints.fromByteArray(slice(sec, pos, 4));
      pos += 4;
      int n = Ints.fromByteArray(slice(sec, pos, 4));
      pos += 4;
      if (target.contains(triIndex)) {
        RoaringBitmap bm = new RoaringBitmap();
        try (java.io.DataInputStream dis =
            new java.io.DataInputStream(new java.io.ByteArrayInputStream(slice(sec, pos, n)))) {
          bm.deserialize(dis);
        }
        acc = (acc == null) ? bm : RoaringBitmap.and(acc, bm);
      }
      pos += n;
    }
    return acc;
  }

  public boolean bloomMightContain(Path bin, long pageOffset, Header h, String traceId)
      throws IOException {
    if (traceId == null || traceId.isEmpty()) return false;
    if (h.lenBloom() == 0) return false;
    long secOff = pageOffset + HEADER_SIZE + h.lenTri() + h.lenLvl();
    byte[] sec = range(bin, secOff, h.lenBloom());
    int n = Ints.fromByteArray(slice(sec, 0, 4));
    try (java.io.ByteArrayInputStream bais =
        new java.io.ByteArrayInputStream(slice(sec, 4, n))) {
      var bloom = com.google.common.hash.BloomFilter.readFrom(
          bais, com.google.common.hash.Funnels.stringFunnel(java.nio.charset.StandardCharsets.UTF_8));
      return bloom.mightContain(traceId);
    }
  }

  public byte[] readDocsSizes(Path bin, long pageOffset, Header h) throws IOException {
    long secOff = pageOffset + HEADER_SIZE + h.lenTri() + h.lenLvl() + h.lenBloom();
    int headLen = 4;
    byte[] head = range(bin, secOff, headLen);
    int nDocs = Ints.fromByteArray(head);
    return range(bin, secOff, headLen + nDocs * 4);
  }

  public List<LogPayloadProto> readDocsByIds(
      Path bin, long pageOffset, Header h, int[] docIds) throws IOException {
    long docsOff = pageOffset + HEADER_SIZE + h.lenTri() + h.lenLvl() + h.lenBloom();
    byte[] sizesTable = readDocsSizes(bin, pageOffset, h);
    int pos = 0;
    int nDocs = Ints.fromByteArray(slice(sizesTable, pos, 4));
    pos += 4;
    int[] sizes = new int[nDocs];
    for (int i = 0; i < nDocs; i++) {
      sizes[i] = Ints.fromByteArray(slice(sizesTable, pos, 4));
      pos += 4;
    }
    long payloadOff = docsOff + 4 + nDocs * 4;

    List<LogPayloadProto> out = new ArrayList<>();
    for (int docId : docIds) {
      long off = payloadOff;
      for (int i = 0; i < docId; i++) off += sizes[i];
      int len = sizes[docId];
      byte[] body = range(bin, off, len);
      stats.bytesRead(len);
      out.add(LogPayloadProto.parseFrom(body));
    }
    stats.docsRead(docIds.length);
    return out;
  }

  public byte[] readRange(Path bin, long offset, int length) throws IOException {
    return range(bin, offset, length);
  }

  private byte[] range(Path bin, long offset, int length) throws IOException {
    FileChannel ch = channelCache.get(bin);
    ByteBuffer bb = ByteBuffer.allocate(length);
    ch.read(bb, offset);
    byte[] bytes = bb.array();
    stats.bytesRead(bytes.length);
    return bytes;
  }

  private static byte[] slice(byte[] a, int off, int len) {
    byte[] r = new byte[len];
    System.arraycopy(a, off, r, 0, len);
    return r;
  }

  @Override
  public void close() throws IOException {
    channelCache.close();
  }

  public record Header(long tsStart, long tsEnd, int maxDocId, int lenTri, int lenLvl, int lenBloom,
      int lenDocs) {}

  private static final class ChannelCache implements Closeable {
    private final int capacity;
    private final LinkedHashMap<Path, FileChannel> map;

    ChannelCache(int capacity) {
      this.capacity = capacity;
      this.map = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Path, FileChannel> eldest) {
          if (size() > ChannelCache.this.capacity) {
            try { eldest.getValue().close(); } catch (IOException ignored) {}
            return true;
          }
          return false;
        }
      };
    }

    synchronized FileChannel get(Path path) throws IOException {
      FileChannel ch = map.get(path);
      if (ch != null && ch.isOpen()) return ch;
      if (ch != null) map.remove(path);
      FileChannel created = FileChannel.open(path, StandardOpenOption.READ);
      map.put(path, created);
      return created;
    }

    @Override
    public synchronized void close() throws IOException {
      for (FileChannel ch : map.values()) {
        try { ch.close(); } catch (IOException ignored) {}
      }
      map.clear();
    }
  }
}
