package org.okapi.logs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "okapi.logs")
@Data
public class LogsConfigProperties {
  // Directory for local persistence before/alongside S3 uploads
  private String dataDir = "logs";

  // Page thresholds
  private int maxPageBytes = 8 * 1024 * 1024; // 8MB
  private int maxDocsPerPage = 20000;
  private long maxPageWindowMs = 120_000; // 2 minutes

  // Buffer pool
  private int maxActivePages = 10000;

  // Bloom filter
  private double bloomFpp = 0.01d;

  // Index
  private boolean fsyncOnPageAppend = true;
  private int indexFsyncEveryPages = 1; // fsync index every N pages

  // S3 settings for querying remote dumps
  private String s3Bucket; // if null/empty, S3 queries are disabled
  private String s3BasePrefix = "logs"; // base prefix for tenant/logStream objects
}
