package org.okapi.logs.query;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.okapi.logs.config.LogsConfigProperties;
import org.okapi.logs.index.PageIndex;
import org.okapi.logs.index.PageIndexEntry;
import org.okapi.logs.io.LogPageSerializer;
import org.okapi.protos.logs.LogPayloadProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Service
@RequiredArgsConstructor
public class S3QueryProcessor implements QueryProcessor {
  private static final Logger log = LoggerFactory.getLogger(S3QueryProcessor.class);
  private final LogsConfigProperties cfg;
  private final MeterRegistry meterRegistry;
  private final S3Client s3Client;

  @Override
  public List<LogPayloadProto> getLogs(
      String tenantId, String logStream, long start, long end, LogFilter filter, QueryConfig qcfg)
      throws IOException {
    if (this.cfg.getS3Bucket() == null || this.cfg.getS3Bucket().isEmpty()) return List.of();
    String prefix = buildPrefix(tenantId, logStream);
    List<LogPayloadProto> out = new ArrayList<>();
    var hrStart = start / 3600_000L;
    var hrEnd = end / 3600_000L;
    for (long hr = hrStart; hr <= hrEnd; hr++) {
      String hourPrefix = prefix + "/" + hr + "/";
      List<String> keys;
      try {
        keys = listObjectKeys(this.cfg.getS3Bucket(), hourPrefix);
      } catch (Exception ex) {
        continue; // hour may not exist
      }
      // find per-node idx files under this hour
      for (String key : keys) {
        if (!key.endsWith("/logfile.idx")) continue;
        String idxKey = key;
        String binKey = key.substring(0, key.length() - ".idx".length()) + ".bin";
        try {
          byte[] idxBytes = getObjectBytes(this.cfg.getS3Bucket(), idxKey);
          List<PageIndexEntry> entries = parseIndex(idxBytes);
          for (PageIndexEntry e : entries) {
            if (e.getTsEnd() < start || e.getTsStart() > end) continue;
            byte[] pageBytes =
                getRangeBytes(this.cfg.getS3Bucket(), binKey, e.getOffset(), e.getLength());
            var page = LogPageSerializer.deserialize(pageBytes);
            out.addAll(FilterEvaluator.apply(page, filter));
          }
        } catch (Exception ex) {
          // Log and skip bad node segment
          log.info(
              "Skipping node segment due to error for index key {}: {}", idxKey, ex.toString());
        }
      }
    }
    return out;
  }

  private String buildPrefix(String tenantId, String logStream) {
    String base = cfg.getS3BasePrefix() == null ? "logs" : cfg.getS3BasePrefix();
    return base + "/" + tenantId + "/" + logStream;
  }

  protected byte[] getObjectBytes(String bucket, String key) throws IOException {
    var resp =
        s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
    byte[] arr = resp.asByteArray();
    meterRegistry.counter("object_storage_fetched_bytes").increment(arr.length);
    return arr;
  }

  protected byte[] getRangeBytes(String bucket, String key, long offset, int length) {
    String range = "bytes=" + offset + "-" + (offset + length - 1);
    var resp =
        s3Client.getObjectAsBytes(
            GetObjectRequest.builder().bucket(bucket).key(key).range(range).build());
    byte[] arr = resp.asByteArray();
    meterRegistry.counter("object_storage_fetched_bytes").increment(arr.length);
    return arr;
  }

  protected List<String> listObjectKeys(String bucket, String prefix) {
    var req =
        software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
            .bucket(bucket)
            .prefix(prefix)
            .build();
    var resp = s3Client.listObjectsV2(req);
    List<String> keys = new ArrayList<>();
    for (var o : resp.contents()) keys.add(o.key());
    return keys;
  }

  private List<PageIndexEntry> parseIndex(byte[] idxBytes) {
    List<PageIndexEntry> out = new ArrayList<>();
    int ENTRY_BYTES = PageIndex.ENTRY_BYTES;
    int pos = 0;
    while (pos + ENTRY_BYTES <= idxBytes.length) {
      byte[] slice = new byte[ENTRY_BYTES];
      System.arraycopy(idxBytes, pos, slice, 0, ENTRY_BYTES);
      pos += ENTRY_BYTES;
      int p = 0;
      long offset = Longs.fromByteArray(copy(slice, p, 8));
      p += 8;
      int length = Ints.fromByteArray(copy(slice, p, 4));
      p += 4;
      long tsStart = Longs.fromByteArray(copy(slice, p, 8));
      p += 8;
      long tsEnd = Longs.fromByteArray(copy(slice, p, 8));
      p += 8;
      int docCount = Ints.fromByteArray(copy(slice, p, 4));
      p += 4;
      int crc = Ints.fromByteArray(copy(slice, p, 4));
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
    return out;
  }

  private static byte[] copy(byte[] a, int off, int len) {
    byte[] r = new byte[len];
    System.arraycopy(a, off, r, 0, len);
    return r;
  }
}
