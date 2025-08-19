package org.okapi.s3;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

public class S3Enhanced {

  S3Client s3;
  S3ByteRangeCache byteRangeCache;

  public S3Enhanced(S3ByteRangeCache byteRangeCache, S3Client s3) {
    this.byteRangeCache = byteRangeCache;
    this.s3 = s3;
  }

  public void deleteDirectory(final String bucket, final String prefix) {
    var key = prefix;
    if (!prefix.endsWith("/")) {
      key = prefix + "/";
    }
    var listed =
        this.s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(key).build());
    for (var obj : listed.contents()) {
      s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(obj.key()).build());
    }
  }

  public List<String> listChildren(final String bucket, final String prefix) {
    var query = prefix;
    if (!prefix.endsWith("/")) {
      query = prefix + "/";
    }
    var objects =
        s3.listObjects(ListObjectsRequest.builder().bucket(bucket).prefix(prefix).build());
    return objects.contents().stream()
        .map(
            summary -> {
              return summary.key();
            })
        .toList();
  }

  public byte[] getRange(String bucket, String key, long start, long end)
      throws IOException, ExecutionException {
    return this.byteRangeCache.getRange(bucket, key, start, end);
  }
}
