package org.okapi.testmodules.guice;

import java.nio.file.Path;
import org.okapi.logs.config.LogsCfg;

public class TestLogsConfig implements LogsCfg {
  private final Path dataDir;
  private final int expectedInsertions;
  private final int maxPageBytes;
  private final long maxPageWindowMs;
  private final int sealedPageCap;
  private final long sealedPageTtlMs;
  private final long idxExpiryDuration;
  private final long bufferPoolFlushEvalMillis;

  public TestLogsConfig(Path dataDir) {
    this(dataDir, 128, 1_048_576, 60_000L, 8, 1_000L, 60_000L, 1_000L);
  }

  public TestLogsConfig(
      Path dataDir,
      int expectedInsertions,
      int maxPageBytes,
      long maxPageWindowMs,
      int sealedPageCap,
      long sealedPageTtlMs,
      long idxExpiryDuration,
      long bufferPoolFlushEvalMillis) {
    this.dataDir = dataDir;
    this.expectedInsertions = expectedInsertions;
    this.maxPageBytes = maxPageBytes;
    this.maxPageWindowMs = maxPageWindowMs;
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
  public String getDataDir() {
    return dataDir.toString();
  }

  @Override
  public int getMaxPageBytes() {
    return maxPageBytes;
  }

  @Override
  public long getMaxPageWindowMs() {
    return maxPageWindowMs;
  }

  @Override
  public String getS3Bucket() {
    return "unused-bucket";
  }

  @Override
  public String getS3BasePrefix() {
    return "logs";
  }

  @Override
  public long getS3UploadGraceMs() {
    return 0;
  }

  @Override
  public long getIdxExpiryDuration() {
    return idxExpiryDuration;
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
  public long getBufferPoolFlushEvalMillis() {
    return bufferPoolFlushEvalMillis;
  }
}
