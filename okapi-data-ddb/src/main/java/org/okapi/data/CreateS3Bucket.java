package org.okapi.data;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

@Slf4j
public class CreateS3Bucket {
  public static void createS3Tables(S3Client s3Client, String env) {
    var buckets =
        new String[] {"credentials-bucket", "index-bucket", "query-log-bucket", "data-bucket"};
    for (var bucket : buckets) {
      var bucketName = "okapi-" + env + "-" + bucket;
      var createBucketRequest = CreateBucketRequest.builder().bucket(bucketName).build();
      try {
        s3Client.createBucket(createBucketRequest);
      } catch (Exception e) {
        log.error("Got exception while creating bucket, mvoing on. {}", e.getMessage());
      }
    }
  }

  public static S3Client getS3Client(String endpoint, String region) {
    var credentials = EnvironmentVariableCredentialsProvider.create();
    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .serviceConfiguration(
            S3Configuration.builder()
                // critical for LocalStack to avoid <bucket>.localhost resolution
                .pathStyleAccessEnabled(true)
                // optional: LocalStack sometimes fails checksum validation for mocked objects
                .checksumValidationEnabled(false)
                .build())
        .credentialsProvider(credentials)
        .build();
  }

  public static void main(String[] args) {
    var env = args[0];
    String endpoint = "http://localhost:4566";

    if (env.equals("prod")) {
      endpoint = "https://s3.eu-west-2.amazonaws.com";
    }
    var s3Client = getS3Client(endpoint, "eu-west-2");
    createS3Tables(s3Client, env);
  }
}
