package org.okapi.byterange;

import org.okapi.s3.ByteRangeSupplier;
import org.okapi.s3.S3ByteRangeCache;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

public class S3ByteRangeSupplier implements ByteRangeSupplier {

  String prefix;
  String bucket;
  S3Client s3Client;
  S3ByteRangeCache byteRangeCache;
  String etag;
  long contentLength;

  public S3ByteRangeSupplier(
      String bucket, String prefix, S3Client s3Client, S3ByteRangeCache byteRangeCache) {
    this.prefix = prefix;
    this.bucket = bucket;
    this.s3Client = s3Client;
    this.byteRangeCache = byteRangeCache;
    var head = HeadObjectRequest.builder().bucket(bucket).key(prefix).build();
    var headResp = s3Client.headObject(head);
    this.etag = headResp.eTag();
    this.contentLength = headResp.contentLength();
  }

  @Override
  public byte[] getBytes(long start, int len) {
    var cached = byteRangeCache.getCachedRange(bucket, prefix, start, start + len, etag);
    if (cached.isPresent()) {
      return cached.get();
    }
    var rangeHeader = "bytes=" + start + "-" + (start + len - 1);
    var getReq = GetObjectRequest.builder().bucket(bucket).key(prefix).range(rangeHeader).build();
    var getResp = s3Client.getObjectAsBytes(getReq);
    var bytes = getResp.asByteArray();
    byteRangeCache.possiblyCache(bucket, prefix, start, start + len, bytes, etag);
    return bytes;
  }

  @Override
  public long getEnd() {
    return contentLength;
  }
}
