package org.okapi.s3;

import static org.okapi.metrics.MetadataFields.CHECKSUM_HEADER;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

public class S3ByteRangeCache {

  public record DataPage(
      String bucket, String key, long pageNumber, byte[] contents, String parentChecksum) {}

  private final LoadingCache<String, DataPage> bufferPool;
  private final S3Client amazonS3;
  private final long pageSize;
  private final Duration expiryDuration;
  Map<String, String> checksums;

  public S3ByteRangeCache(S3Client amazonS3, int maxCachedPages, Duration expiryDuration) {
    this(amazonS3, maxCachedPages, 16 * 1024, expiryDuration); // default to 16 KB
  }

  public S3ByteRangeCache(
      S3Client amazonS3, int maxCachedPages, long pageSize, Duration expiryDuration) {
    this.amazonS3 = amazonS3;
    this.pageSize = pageSize;
    this.expiryDuration = expiryDuration;
    this.checksums = new ConcurrentHashMap<>();
    this.bufferPool =
        CacheBuilder.<String, DataPage>newBuilder()
            .maximumSize(maxCachedPages)
            .expireAfterAccess(expiryDuration)
            .build(
                new CacheLoader<String, DataPage>() {
                  @Override
                  public DataPage load(String key) throws Exception {
                    String[] parts = key.split("::");
                    long pageNumber = Long.parseLong(parts[2]);
                    byte[] contents = getPage(parts[0], parts[1], pageNumber);
                    var bucketName = parts[0];
                    var s3Prefix = parts[1];
                    var headRequest =
                        HeadObjectRequest.builder().bucket(bucketName).key(s3Prefix).build();
                    var metadata = amazonS3.headObject(headRequest);
                    var checkSum = metadata.metadata().get(CHECKSUM_HEADER);
                    return new DataPage(bucketName, s3Prefix, pageNumber, contents, checkSum);
                  }
                });
  }

  private String getCacheKey(String bucket, String key, long page) {
    return bucket + "::" + key + "::" + page;
  }

  public byte[] getRange(String bucket, String key, long start, long end)
      throws ExecutionException {
    if (end <= start) {
      throw new IllegalArgumentException("End must be greater than start");
    }

    long[] pagedRange = getPagedRange(start, end);
    long pageBoundary = start;
    int nBytes = Math.toIntExact(end - start);
    byte[] buffer = new byte[nBytes];
    int bufferBoundary = 0;

    for (long pageNum = pagedRange[0]; pageNum < pagedRange[1]; pageNum++) {
      long pageStart = pageSize * pageNum;
      long pageEnd = pageStart + pageSize;
      String cacheKey = getCacheKey(bucket, key, pageNum);
      DataPage page = bufferPool.get(cacheKey);

      long copyStart = pageBoundary - pageStart;
      long toCopy = Math.min(end, pageEnd) - pageBoundary;

      System.arraycopy(
          page.contents(),
          Math.toIntExact(copyStart),
          buffer,
          bufferBoundary,
          Math.toIntExact(toCopy));

      bufferBoundary += toCopy;
      pageBoundary += toCopy;
    }

    return buffer;
  }

  private long[] getPagedRange(long start, long end) {
    long pageStart = start / pageSize;
    long pageEnd = (end + pageSize - 1) / pageSize;
    return new long[] {pageStart, pageEnd};
  }

  private byte[] getPage(String bucket, String key, long page) throws IOException {
    long pageStart = page * pageSize;
    long pageEndInclusive = pageStart + pageSize - 1;

    GetObjectRequest request =
        GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .range("bytes=" + pageStart + "-" + pageEndInclusive)
            .build();
    return amazonS3.getObject(request).readAllBytes();
  }
}
