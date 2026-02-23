package org.okapi.logs.query;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class QueryConfig {
  public final boolean s3;
  public final boolean bufferPool;
  public final boolean disk;
  public final boolean fanOut;

  public QueryConfig(boolean s3, boolean bufferPool, boolean disk, boolean fanOut) {
    this.s3 = s3;
    this.bufferPool = bufferPool;
    this.disk = disk;
    this.fanOut = fanOut;
  }

  public static QueryConfig localSources() {
    return new QueryConfig(false, true, true, false);
  }

  public static QueryConfig allSources() {
    return new QueryConfig(true, true, true, true);
  }
}
