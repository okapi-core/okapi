package org.okapi.logs.io;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import org.okapi.protos.logs.LogPayloadProto;
import org.roaringbitmap.RoaringBitmap;

public final class LogPageSerializer {

  private LogPageSerializer() {}

  /** data layout on page */
  // Header layout (big-endian):
  // [0..7]   tsStart (int64)
  // [8..15]  tsEnd (int64)
  // [16..19] maxDocId (int32)
  // [20..23] len_trigramMap (int32)
  // [24..27] len_levelsInPage (int32)
  // [28..31] len_traceIdSet (int32)
  // [32..35] len_logDocs (int32)
  // [36..55] reserved (6 * int32 = 24 bytes)
  // [56..75] reserved (20 bytes)
  // sections ...
  // [end..end+3] CRC32 of all previous bytes

  public static byte[] serialize(LogPage page) throws IOException {
    byte[] tri = serializeTrigramMap(page.getTrigramMap());
    byte[] lvl = serializeLevels(page.getLevelsInPage());
    byte[] bloom = serializeBloom(page.getTraceIdSet());
    byte[] docs = serializeLogDocs(page.getLogDocs());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    // header
    baos.write(Longs.toByteArray(page.getTsStart()));
    baos.write(Longs.toByteArray(page.getTsEnd()));
    baos.write(Ints.toByteArray(page.getMaxDocId()));
    baos.write(Ints.toByteArray(tri.length));
    baos.write(Ints.toByteArray(lvl.length));
    baos.write(Ints.toByteArray(bloom.length));
    baos.write(Ints.toByteArray(docs.length));
    // 6 unused int32 slots
    for (int i = 0; i < 6; i++) baos.write(Ints.toByteArray(0));
    // 20 reserved bytes (write ASCII magic then zeros)
    byte[] magic = "LPv1".getBytes(StandardCharsets.US_ASCII);
    byte[] reserved = new byte[20];
    System.arraycopy(magic, 0, reserved, 0, magic.length);
    baos.write(reserved);

    // sections
    baos.write(tri);
    baos.write(lvl);
    baos.write(bloom);
    baos.write(docs);

    // CRC32 over everything so far
    byte[] body = baos.toByteArray();
    CRC32 crc = new CRC32();
    crc.update(body);
    baos.write(Ints.toByteArray((int) crc.getValue()));
    return baos.toByteArray();
  }

  private static byte[] serializeTrigramMap(Map<Integer, RoaringBitmap> trigramMap)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (Map.Entry<Integer, RoaringBitmap> e : trigramMap.entrySet()) {
      baos.write(Ints.toByteArray(e.getKey()));
      ByteArrayOutputStream bmOut = new ByteArrayOutputStream();
      try (DataOutputStream dos = new DataOutputStream(bmOut)) {
        e.getValue().serialize(dos);
      }
      byte[] bmBytes = bmOut.toByteArray();
      baos.write(Ints.toByteArray(bmBytes.length));
      baos.write(bmBytes);
    }
    return baos.toByteArray();
  }

  private static byte[] serializeLevels(Map<Integer, RoaringBitmap> levels) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (Map.Entry<Integer, RoaringBitmap> e : levels.entrySet()) {
      baos.write(Ints.toByteArray(e.getKey()));
      ByteArrayOutputStream bmOut = new ByteArrayOutputStream();
      try (DataOutputStream dos = new DataOutputStream(bmOut)) {
        e.getValue().serialize(dos);
      }
      byte[] bmBytes = bmOut.toByteArray();
      baos.write(Ints.toByteArray(bmBytes.length));
      baos.write(bmBytes);
    }
    return baos.toByteArray();
  }

  private static byte[] serializeBloom(BloomFilter<CharSequence> bloom) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bloom.writeTo(baos);
    byte[] payload = baos.toByteArray();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(Ints.toByteArray(payload.length));
    out.write(payload);
    return out.toByteArray();
  }

  private static byte[] serializeLogDocs(List<LogPayloadProto> docs) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(Ints.toByteArray(docs.size()));
    // sizes table
    List<byte[]> encoded = new ArrayList<>(docs.size());
    for (LogPayloadProto doc : docs) {
      byte[] b = doc.toByteArray();
      encoded.add(b);
      baos.write(Ints.toByteArray(b.length));
    }
    // payloads
    for (byte[] b : encoded) {
      baos.write(b);
    }
    return baos.toByteArray();
  }

  public static LogPage deserialize(byte[] pageBytes) throws IOException {
    // Verify CRC
    if (pageBytes.length < 76 + 4) throw new IOException("page too small");
    int crcOffset = pageBytes.length - 4;
    int expectedCrc =
        Ints.fromByteArray(
            new byte[] {
              pageBytes[crcOffset],
              pageBytes[crcOffset + 1],
              pageBytes[crcOffset + 2],
              pageBytes[crcOffset + 3]
            });
    CRC32 crc = new CRC32();
    crc.update(pageBytes, 0, crcOffset);
    if ((int) crc.getValue() != expectedCrc) {
      throw new IOException("CRC32 mismatch for page");
    }

    int pos = 0;
    long tsStart = Longs.fromByteArray(slice(pageBytes, pos, 8));
    pos += 8;
    long tsEnd = Longs.fromByteArray(slice(pageBytes, pos, 8));
    pos += 8;
    int maxDocId = Ints.fromByteArray(slice(pageBytes, pos, 4));
    pos += 4;
    int lenTri = Ints.fromByteArray(slice(pageBytes, pos, 4));
    pos += 4;
    int lenLvl = Ints.fromByteArray(slice(pageBytes, pos, 4));
    pos += 4;
    int lenBloom = Ints.fromByteArray(slice(pageBytes, pos, 4));
    pos += 4;
    int lenDocs = Ints.fromByteArray(slice(pageBytes, pos, 4));
    pos += 4;
    // skip remaining 6 ints + 20 bytes
    pos += (6 * 4) + 20;

    byte[] triSec = slice(pageBytes, pos, lenTri);
    pos += lenTri;
    byte[] lvlSec = slice(pageBytes, pos, lenLvl);
    pos += lenLvl;
    byte[] bloomSec = slice(pageBytes, pos, lenBloom);
    pos += lenBloom;
    byte[] docsSec = slice(pageBytes, pos, lenDocs);

    // Reconstruct LogPage
    LogPage.LogPageBuilder builder = LogPage.builder();
    deserializeTrigramMap(triSec, builder);
    deserializeLevels(lvlSec, builder);
    BloomFilter<CharSequence> bloom = deserializeBloom(bloomSec);
    builder.traceIdSet(bloom);
    List<LogPayloadProto> docs = deserializeDocs(docsSec);
    for (LogPayloadProto d : docs) builder.doc(d);
    LogPage page = builder.expectedInsertions(maxDocId + 1).build();
    page.setTsStart(tsStart);
    page.setTsEnd(tsEnd);
    page.setMaxDocId(maxDocId);
    return page;
  }

  private static void deserializeTrigramMap(byte[] sec, LogPage.LogPageBuilder builder)
      throws IOException {
    int pos = 0;
    while (pos < sec.length) {
      int trigramIndex = Ints.fromByteArray(slice(sec, pos, 4));
      pos += 4;
      int n = Ints.fromByteArray(slice(sec, pos, 4));
      pos += 4;
      RoaringBitmap bm = new RoaringBitmap();
      bm.deserialize(new java.io.DataInputStream(new ByteArrayInputStream(slice(sec, pos, n))));
      pos += n;
      builder.trigramEntry(trigramIndex, bm);
    }
  }

  private static void deserializeLevels(byte[] sec, LogPage.LogPageBuilder builder)
      throws IOException {
    int pos = 0;
    while (pos < sec.length) {
      int levelCode = Ints.fromByteArray(slice(sec, pos, 4));
      pos += 4;
      int n = Ints.fromByteArray(slice(sec, pos, 4));
      pos += 4;
      RoaringBitmap bm = new RoaringBitmap();
      bm.deserialize(new java.io.DataInputStream(new ByteArrayInputStream(slice(sec, pos, n))));
      pos += n;
      builder.levelEntry(levelCode, bm);
    }
  }

  private static BloomFilter<CharSequence> deserializeBloom(byte[] sec) throws IOException {
    int pos = 0;
    int n = Ints.fromByteArray(slice(sec, pos, 4));
    pos += 4;
    ByteArrayInputStream bais = new ByteArrayInputStream(slice(sec, pos, n));
    return BloomFilter.readFrom(bais, Funnels.stringFunnel(StandardCharsets.UTF_8));
  }

  private static List<LogPayloadProto> deserializeDocs(byte[] sec) throws IOException {
    int pos = 0;
    int nDocs = Ints.fromByteArray(slice(sec, pos, 4));
    pos += 4;
    int[] sizes = new int[nDocs];
    for (int i = 0; i < nDocs; i++) {
      sizes[i] = Ints.fromByteArray(slice(sec, pos, 4));
      pos += 4;
    }
    List<LogPayloadProto> docs = new ArrayList<>(nDocs);
    for (int i = 0; i < nDocs; i++) {
      byte[] b = slice(sec, pos, sizes[i]);
      pos += sizes[i];
      docs.add(LogPayloadProto.parseFrom(b));
    }
    return docs;
  }

  private static byte[] slice(byte[] a, int off, int len) {
    byte[] r = new byte[len];
    System.arraycopy(a, off, r, 0, len);
    return r;
  }
}
