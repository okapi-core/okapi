package org.okapi.metrics.config;

public interface MetricsCfg {
  int getExpectedInsertions();

  long getMaxPageWindowMs();

  int getMaxPageBytes();

  double getBloomFpp();

  int getSealedPageCap();

  long getSealedPageTtlMs();

  String getDataDir();

  long getIdxExpiryDuration();

  long getS3UploadGraceMs();

  String getS3Bucket();

  String getS3BasePrefix();

  long getBufferPoolFlushEvalMillis();
}
