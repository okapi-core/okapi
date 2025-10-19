package org.okapi.traces.spring;

import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class AwsConfiguration {

  @Bean
  public Region region(@Value("${okapi.traces.s3.region}") String region) {
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
      @Value("${okapi.traces.s3.endpoint:http://localhost:4566}") String endpoint) {
    return S3Client.builder()
        .credentialsProvider(credentialsProvider)
        .region(region)
        .endpointOverride(URI.create(endpoint))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }
}
