package org.okapi.logs.config;

public class ModifiableCfg extends LogsCfgImpl {
  public ModifiableCfg(
      String dataDir,
      int maxPageBytes,
      int maxDocsPerPage,
      long maxPageWindowMs,
      String s3Bucket,
      String s3BasePrefix,
      long s3UploadGraceMs,
      long idxExpiryDuration) {
    super(
        dataDir,
        maxPageBytes,
        maxDocsPerPage,
        maxPageWindowMs,
        s3Bucket,
        s3BasePrefix,
        s3UploadGraceMs,
        idxExpiryDuration);
  }

  public ModifiableCfg(String s3Bucket) {
    super(s3Bucket);
  }

  public void setDataDir(String dataDir) {
    this.dataDir = dataDir;
  }

  public void setS3Bucket(String bucket) {
    this.s3Bucket = bucket;
  }

  public void setS3BasePrefix(String prefix) {
    this.s3BasePrefix = prefix;
  }

  public void setS3UploadGraceMs(long graceMs) {
    this.s3UploadGraceMs = graceMs;
  }

  public void setMaxDocsPerPage(int maxDocsPerPage) {
    this.maxDocsPerPage = maxDocsPerPage;
  }

  public void setMaxPageBytes(int bytes) {
    this.maxPageBytes = bytes;
  }

  public void setMaxPageWindowMs(long ms) {
    this.maxPageWindowMs = ms;
  }
}
