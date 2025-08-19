package org.okapi.metrics.wal;

import java.util.*;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.wal.Wal.MetricEvent;
import org.okapi.wal.Wal.WalRecord;
import org.okapi.wal.WalStreamConsumer;

/**
 * Applies WalRecord (MetricEventBatch) to a ShardMap during recovery. Decoupled from okapi-wal so
 * WAL remains reusable for logs/traces.
 *
 * <p>Contract: - Idempotent by LSN via lastAppliedLsn(). - Uses the same sharding policy as live
 * ingestion via ShardsAndSeriesAssigner.
 */
public final class MetricsWalStreamConsumer implements WalStreamConsumer {

  private final ShardMap shardMap;
  private final ShardsAndSeriesAssigner assigner;
  private final String selfNode;
  private volatile long lastAppliedLsn;

  public MetricsWalStreamConsumer(
      ShardMap shardMap,
      ShardsAndSeriesAssigner assigner,
      String selfNode,
      long initialWatermark) {
    this.shardMap = shardMap;
    this.assigner = assigner;
    this.selfNode = selfNode;
    this.lastAppliedLsn = initialWatermark;
  }

  @Override
  public long lastAppliedLsn() {
    return lastAppliedLsn;
  }

  @Override
  public void consume(long lsn, WalRecord record) throws Exception {
    if (lsn <= lastAppliedLsn) return; // idempotent skip
    if (!record.hasEvent()) return;

    for (MetricEvent e : record.getEvent().getEventsList()) {
      String metricName = e.getName();
      Map<String, String> tags = e.getTagsMap();
      long[] ts = e.getTsList().stream().mapToLong(Long::longValue).toArray();
      float[] vals = new float[e.getValsCount()];
      for (int i = 0; i < vals.length; i++) vals[i] = e.getVals(i);

      // Build a consistent series path for sharding. We sort tags for stability.
      String path = buildPath(metricName, tags);
      int shard = assigner.getShard(path);
      String node = assigner.getNode(shard);
      // if sharding topology changed, we still apply locally; operators should warm-restore with
      // consistent topology.
      if (!Objects.equals(node, selfNode)) {
        // In recovery we still apply locally to maintain completeness; alternatively, log and
        // continue.
      }
      MetricsContext ctx = MetricsContext.createContext(UUID.randomUUID().toString());
      shardMap.get(shard).writeBatch(ctx, path, ts, vals);
    }

    lastAppliedLsn = lsn;
  }

  private static String buildPath(String metricName, Map<String, String> tags) {
    // Stable tag encoding: name|k1=v1|k2=v2 ... with keys sorted
    if (tags == null || tags.isEmpty()) return metricName;
    List<Map.Entry<String, String>> list = new ArrayList<>(tags.entrySet());
    list.sort(Map.Entry.comparingByKey());
    StringBuilder sb = new StringBuilder(metricName);
    for (Map.Entry<String, String> e : list) {
      sb.append('|').append(e.getKey()).append('=').append(e.getValue());
    }
    return sb.toString();
  }
}
