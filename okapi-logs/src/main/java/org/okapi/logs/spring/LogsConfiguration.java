package org.okapi.logs.spring;

import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.config.LogsCfgImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogsConfiguration {

  @Bean
  public LogsCfg getLogsCfg(
      @Value("${okapi.logs.dataDir}") String dataDir,
      @Value("${okapi.logs.maxPageBytes}") int maxPageBytes,
      @Value("${okapi.logs.maxDocsPerPage}") int maxDocsPerPage,
      @Value("${okapi.logs.maxPageWindowMs}") long maxPageWindowMs,
      @Value("${okapi.logs.s3Bucket}") String s3Bucket,
      @Value("${okapi.logs.s3BasePrefix}") String s3BasePrefix,
      @Value("${okapi.logs.s3UploadGraceMs}") long s3UploadGraceMs,
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
}
