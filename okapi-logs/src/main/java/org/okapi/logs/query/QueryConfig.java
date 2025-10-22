package org.okapi.logs.query;

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

  public static QueryConfig defaultConfig() {
    return new QueryConfig(true, true, true, false);
  }

  public static QueryConfig fanOutConfig() {
    return new QueryConfig(false, true, true, true);
  }
}

