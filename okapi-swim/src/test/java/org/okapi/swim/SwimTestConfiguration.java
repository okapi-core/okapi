package org.okapi.swim;

import org.okapi.swim.membership.SwimMembershipProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class SwimTestConfiguration {

  @Bean
  public SwimMembershipProperties swimMembershipProperties() {
    return new SwimMembershipProperties("swim-test", "test-bucket");
  }

  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .region(Region.EU_WEST_2)
        .endpointOverride(URI.create("http://localhost:4566"))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }
}
