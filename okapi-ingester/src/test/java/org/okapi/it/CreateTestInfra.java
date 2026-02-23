package org.okapi.it;

import lombok.AllArgsConstructor;
import org.okapi.testutils.OkapiTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

@AllArgsConstructor
public class CreateTestInfra {

  String logsBucket;
  String tracesBucket;

  public void createBucketIfNotExists(S3Client client, String bucket) {
    var allBuckets = client.listBuckets().buckets().stream().map(Bucket::name).toList();
    if (allBuckets.contains(bucket)) return;
    client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
  }

  public void createBuckets() {
    var s3Client = OkapiTestUtils.getLocalstackS3Client();
    createBucketIfNotExists(s3Client, logsBucket);
    createBucketIfNotExists(s3Client, tracesBucket);
  }

  public void createAllInfra() {
    createBuckets();
  }
}
