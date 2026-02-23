package org.okapi.testmodules.guice;

import java.nio.file.Path;
import org.okapi.metrics.config.MetricsCfg;

public class TestMetricsConfig implements MetricsCfg {
  private final Path dataDir;
  private final int expectedInsertions;
  private final long maxPageWindowMs;
  private final int maxPageBytes;
  private final double bloomFpp;
  private final int sealedPageCap;
  private final long sealedPageTtlMs;
  private final long idxExpiryDuration;
  private final long bufferPoolFlushEvalMillis;

  public TestMetricsConfig(Path dataDir) {
    this(dataDir, 128, 60_000L, 1_048_576, 0.01d, 8, 1_000L, 60_000L, 1_000L);
  }

  public TestMetricsConfig(
      Path dataDir,
      int expectedInsertions,
      long maxPageWindowMs,
      int maxPageBytes,
      double bloomFpp,
      int sealedPageCap,
      long sealedPageTtlMs,
      long idxExpiryDuration,
      long bufferPoolFlushEvalMillis) {
    this.dataDir = dataDir;
    this.expectedInsertions = expectedInsertions;
    this.maxPageWindowMs = maxPageWindowMs;
    this.maxPageBytes = maxPageBytes;
    this.bloomFpp = bloomFpp;
    this.sealedPageCap = sealedPageCap;
    this.sealedPageTtlMs = sealedPageTtlMs;
    this.idxExpiryDuration = idxExpiryDuration;
    this.bufferPoolFlushEvalMillis = bufferPoolFlushEvalMillis;
  }

  @Override
  public int getExpectedInsertions() {
    return expectedInsertions;
  }

  @Override
  public long getMaxPageWindowMs() {
    return maxPageWindowMs;
  }

  @Override
  public int getMaxPageBytes() {
    return maxPageBytes;
  }

  @Override
  public double getBloomFpp() {
    return bloomFpp;
  }

  @Override
  public int getSealedPageCap() {
    return sealedPageCap;
  }

  @Override
  public long getSealedPageTtlMs() {
    return sealedPageTtlMs;
  }

  @Override
  public String getDataDir() {
    return dataDir.toString();
  }

  @Override
  public long getIdxExpiryDuration() {
    return idxExpiryDuration;
  }

  @Override
  public long getS3UploadGraceMs() {
    return 0;
  }

  @Override
  public String getS3Bucket() {
    return "unused-bucket";
  }

  @Override
  public String getS3BasePrefix() {
    return "metrics";
  }

  @Override
  public long getBufferPoolFlushEvalMillis() {
    return bufferPoolFlushEvalMillis;
  }
}
