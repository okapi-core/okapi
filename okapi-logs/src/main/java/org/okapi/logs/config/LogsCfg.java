package org.okapi.logs.config;

public interface LogsCfg {
    String getDataDir();
    int getMaxPageBytes();
    int getMaxDocsPerPage();
    long getMaxPageWindowMs();
    String getS3Bucket();
    String getS3BasePrefix();
    long getS3UploadGraceMs();
    long getIdxExpiryDuration();
}
