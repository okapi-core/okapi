package org.okapi.metrics.service;

import java.util.Map;
import java.util.UUID;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.wal.Wal.MetricEvent;
import org.okapi.wal.Wal.WalRecord;
import org.okapi.wal.WalStreamConsumer;

/**
 * Applies WalRecord (MetricEventBatch) into a ShardMap in an idempotent way, keyed by LSN
 * watermark.
 */
public final class ShardMapWalConsumer implements WalStreamConsumer {

  private final ShardMap shardMap;
  private final String tenantId;
  private final String nodeId;
  public final ShardsAndSeriesAssigner assigner;

  /** External watermark (e.g., from persisted.lsn or snapshot). */
  private volatile long lastAppliedLsn;

  public ShardMapWalConsumer(
      ShardMap shardMap,
      long initialWatermark,
      String tenantId,
      String nodeId,
      ShardsAndSeriesAssigner assigner) {
    this.shardMap = shardMap;
    this.lastAppliedLsn = initialWatermark;
    this.tenantId = tenantId;
    this.nodeId = nodeId;
    this.assigner = assigner;
  }

  @Override
  public void consume(long lsn, WalRecord record) throws Exception {
    // replayed in a way that data constraints are violated.
    if (lsn <= lastAppliedLsn) {
      return; // idempotent skip
    }
    if (!record.hasEvent()) return;

    for (MetricEvent e : record.getEvent().getEventsList()) {
      String metricName = e.getName();
      long[] ts = e.getTsList().stream().mapToLong(Long::longValue).toArray();
      float[] vals = new float[e.getValsCount()];
      for (int i = 0; i < vals.length; i++) vals[i] = e.getVals(i);
      Map<String, String> tags = e.getTagsMap();

      MetricsContext ctx = MetricsContext.createContext(UUID.randomUUID().toString());
      // route all points for this event into shard 0 by default; production code should re-use your
      // assigner
      var path = MetricPaths.convertToPath(e.getTenantId(), metricName, e.getTagsMap());
      var assigned = this.assigner.getShard(path);
      shardMap.get(assigned).writeBatch(ctx, metricName, ts, vals);
    }

    lastAppliedLsn = lsn;
  }

  @Override
  public long lastAppliedLsn() {
    return lastAppliedLsn;
  }
}
