package org.okapi.metrics.s3;

public class S3Prefixes {
  public static final String shardCheckpointPrefix(String opId, int shard) {
    return "okapi/shard-checkpoint/" + opId + "/" + shard;
  }

  public static final String hourlyPrefixRoot(long epoch, String tenantId) {
    return "okapi/" + tenantId + "/" + "hourly/" + epoch;
  }

  public static final String hourlyPrefix(String tenantId, long epoch, int shard) {
    return hourlyPrefixRoot(epoch, tenantId) + "/" + shard + "/checkpoint.ckpt";
  }

  public static final String parquetPrefix(String tenantId, long epoch) {
    return "okapi/" + tenantId + "/parquet/" + epoch + "/" + epoch + ".parquet";
  }
}
