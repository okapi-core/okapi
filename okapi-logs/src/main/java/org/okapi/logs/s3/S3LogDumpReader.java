package org.okapi.logs.s3;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.okapi.protos.logs.LogPayloadProto;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@RequiredArgsConstructor
public class S3LogDumpReader {
  private static final int HEADER_SIZE = 80; // bytes
  private final S3Client s3;
  private final MeterRegistry meterRegistry;

  public Header readHeader(String bucket, String key, long offset) {
    // Header is fixed 80 bytes (see LogPageSerializer)
    byte[] hdr = range(bucket, key, offset, HEADER_SIZE);
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
    // ignore reserved
    return new Header(tsStart, tsEnd, maxDocId, lenTri, lenLvl, lenBloom, lenDocs);
  }

  public byte[] readLevelsSection(String bucket, String key, long pageOffset, Header h) {
    long secOff = pageOffset + HEADER_SIZE + h.lenTri;
    return range(bucket, key, secOff, h.lenLvl);
  }

  public byte[] readBloomSection(String bucket, String key, long pageOffset, Header h) {
    long secOff = pageOffset + HEADER_SIZE + h.lenTri + h.lenLvl;
    return range(bucket, key, secOff, h.lenBloom);
  }

  public byte[] readDocsSizes(String bucket, String key, long pageOffset, Header h) {
    long secOff = pageOffset + HEADER_SIZE + h.lenTri + h.lenLvl + h.lenBloom;
    // First 4 + sizes table (nDocs * 4)
    byte[] head = range(bucket, key, secOff, 4);
    int nDocs = Ints.fromByteArray(head);
    return range(bucket, key, secOff, 4 + nDocs * 4);
  }

  public List<LogPayloadProto> readDocsByIds(
      String bucket, String key, long pageOffset, Header h, int[] docIds) throws IOException {
    long docsOff = pageOffset + HEADER_SIZE + h.lenTri + h.lenLvl + h.lenBloom;
    byte[] sizesTable = readDocsSizes(bucket, key, pageOffset, h);
    int pos = 0;
    int nDocs = Ints.fromByteArray(slice(sizesTable, pos, 4));
    pos += 4;
    int[] sizes = new int[nDocs];
    for (int i = 0; i < nDocs; i++) {
      sizes[i] = Ints.fromByteArray(slice(sizesTable, pos, 4));
      pos += 4;
    }
    long payloadOff = docsOff + 4 + nDocs * 4;

    // Build ranges to coalesce adjacent reads
    List<LogPayloadProto> out = new ArrayList<>();
    for (int docId : docIds) {
      long off = payloadOff;
      for (int i = 0; i < docId; i++) off += sizes[i];
      int len = sizes[docId];
      byte[] body = range(bucket, key, off, len);
      out.add(LogPayloadProto.parseFrom(body));
    }
    return out;
  }

  private byte[] range(String bucket, String key, long offset, int length) {
    String r = "bytes=" + offset + "-" + (offset + length - 1);
    ResponseBytes<GetObjectResponse> resp =
        s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).range(r).build());
    byte[] bytes = resp.asByteArray();
    if (meterRegistry != null) {
      meterRegistry.counter("object_storage_fetched_bytes").increment(bytes.length);
    }
    return bytes;
  }

  private static byte[] slice(byte[] a, int off, int len) {
    byte[] r = new byte[len];
    System.arraycopy(a, off, r, 0, len);
    return r;
  }

  public record Header(long tsStart, long tsEnd, int maxDocId, int lenTri, int lenLvl, int lenBloom,
      int lenDocs) {}
}
