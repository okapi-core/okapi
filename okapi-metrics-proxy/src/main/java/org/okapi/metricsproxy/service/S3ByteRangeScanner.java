package org.okapi.metricsproxy.service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.okapi.metrics.scanning.ByteRangeScanner;
import org.okapi.s3.S3ByteRangeCache;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

public class S3ByteRangeScanner implements ByteRangeScanner {
  String bucket;
  String key;
  S3Client s3;
  S3ByteRangeCache byteRangeCache;

  long totalSize;

  public S3ByteRangeScanner(
      String bucket, String key, S3Client s3, S3ByteRangeCache byteRangeCache) {
    this.bucket = bucket;
    this.key = key;
    this.s3 = s3;
    this.byteRangeCache = byteRangeCache;

    this.totalSize =
        s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).contentLength();
  }

  @Override
  public long totalBytes() throws IOException {
    return this.totalSize;
  }

  @Override
  public byte[] getRange(long off, int nb) throws ExecutionException {
    return byteRangeCache.getRange(bucket, key, off, off + nb);
  }
}
