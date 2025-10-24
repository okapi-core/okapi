package org.okapi.logs.spring;

import java.net.URI;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Slf4j
@Configuration
public class AwsConfiguration {
  @Bean
  public Region region(@Value("${okapi.logs.s3.region}") String region) {
    return Region.of(region);
  }

  @Bean
  public AwsCredentialsProvider credentialsProvider() {
    return EnvironmentVariableCredentialsProvider.create();
  }

  @Bean
  @Profile("prod")
  public S3Client s3Client(
      @Autowired AwsCredentialsProvider credentialsProvider, @Autowired Region region) {
    return S3Client.builder().credentialsProvider(credentialsProvider).region(region).build();
  }

  @Bean
  @Profile("test")
  public S3Client s3ClientLocalstack(
      @Autowired AwsCredentialsProvider credentialsProvider,
      @Autowired Region region,
      @Value("${okapi.logs.s3.bucket}") String s3Bucket,
      @Value("${okapi.swim.s3.bucket}") String swimBucket) {
    var client =
        S3Client.builder()
            .credentialsProvider(credentialsProvider)
            .region(region)
            .endpointOverride(URI.create("http://localhost:4566"))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();

    var toCreate = Arrays.asList(s3Bucket, swimBucket);
    for (var bucket : toCreate) {
      var doesBucketExist = HeadBucketRequest.builder().bucket(bucket).build();
      try {
        var res = client.headBucket(doesBucketExist);
      } catch (AwsServiceException e) {
        log.info("Got exception while doing head bucket. {}", e.getMessage());
        log.info("Creating bucket. {}", bucket);
        var req = CreateBucketRequest.builder().bucket(bucket).build();
        client.createBucket(req);
      }
    }

    return client;
  }

  @Bean
  @Profile("k8s")
  public S3Client s3ClientK8s(
      @Autowired AwsCredentialsProvider credentialsProvider,
      @Autowired Region region,
      @org.springframework.beans.factory.annotation.Value("${okapi.logs.s3.endpoint:}")
          String endpoint) {
    var builder = S3Client.builder().credentialsProvider(credentialsProvider).region(region);
    if (endpoint != null && !endpoint.isBlank()) {
      builder =
          builder
              .endpointOverride(URI.create(endpoint))
              .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
    }
    return builder.build();
  }
}
