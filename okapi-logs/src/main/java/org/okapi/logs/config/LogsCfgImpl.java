package org.okapi.logs.config;

import lombok.Getter;

@Getter
public class LogsCfgImpl implements LogsCfg {
  // Directory for local persistence before/alongside S3 uploads
  protected String dataDir = "logs";

  // Page thresholds
  protected int maxPageBytes = 8 * 1024 * 1024; // 8MB
  protected int maxDocsPerPage = 20000;
  protected long maxPageWindowMs = 120_000; // 2 minutes

  // S3 settings for querying remote dumps
  protected String s3Bucket; // if null/empty, S3 queries are disabled
  protected String s3BasePrefix = "logs"; // base prefix for tenant/logStream objects

  // S3 upload scheduler
  protected long s3UploadGraceMs = 120_000L; // 2 minutes past the hour
  protected long idxExpiryDuration = 3600_000L;

  public LogsCfgImpl(
      String dataDir,
      int maxPageBytes,
      int maxDocsPerPage,
      long maxPageWindowMs,
      String s3Bucket,
      String s3BasePrefix,
      long s3UploadGraceMs,
      long idxExpiryDuration) {
    this.dataDir = dataDir;
    this.maxPageBytes = maxPageBytes;
    this.maxDocsPerPage = maxDocsPerPage;
    this.maxPageWindowMs = maxPageWindowMs;
    this.s3Bucket = s3Bucket;
    this.s3BasePrefix = s3BasePrefix;
    this.s3UploadGraceMs = s3UploadGraceMs;
    this.idxExpiryDuration = idxExpiryDuration;
  }

  private LogsCfgImpl() {}

  public LogsCfgImpl(String s3Bucket) {
    this();
    this.s3Bucket = s3Bucket;
  }
}
