package org.okapi.logs.spring;

import com.google.gson.Gson;
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.config.LogsCfgImpl;
import org.okapi.swim.membership.MembershipEventPublisher;
import org.okapi.swim.membership.S3MembershipEventPublisher;
import org.okapi.swim.membership.SwimMembershipProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class LogsConfiguration {

  @Bean
  public LogsCfg getLogsCfg(
      @Value("${okapi.logs.dataDir}") String dataDir,
      @Value("${okapi.logs.maxPageBytes}") int maxPageBytes,
      @Value("${okapi.logs.maxDocsPerPage}") int maxDocsPerPage,
      @Value("${okapi.logs.maxPageWindowMs}") long maxPageWindowMs,
      @Value("${okapi.logs.s3.bucket}") String s3Bucket,
      @Value("${okapi.logs.s3.basePrefix}") String s3BasePrefix,
      @Value("${okapi.logs.s3.uploadGraceMs}") long s3UploadGraceMs,
      @Value("${okapi.logs.idxExpiryDuration}") long idxExpiryDuration) {
    return new LogsCfgImpl(
        dataDir,
        maxPageBytes,
        maxDocsPerPage,
        maxPageWindowMs,
        s3Bucket,
        s3BasePrefix,
        s3UploadGraceMs,
        idxExpiryDuration);
  }

  @Bean
  public SwimMembershipProperties swimMembershipProperties(
      @Value("${okapi.swim.svcName}") String svcName,
      @Value("${okapi.swim.s3.bucket}") String swimBucket) {
    return new SwimMembershipProperties(svcName, swimBucket);
  }

  @Bean
  public MembershipEventPublisher membershipEventPublisher(
      @Autowired SwimMembershipProperties membershipProperties, @Autowired S3Client client) {
    return new S3MembershipEventPublisher(membershipProperties, client, new Gson());
  }
}
