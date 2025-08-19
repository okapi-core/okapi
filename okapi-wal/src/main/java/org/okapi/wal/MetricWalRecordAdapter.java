package org.okapi.wal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import org.okapi.wal.Wal.MetricEvent;
import org.okapi.wal.Wal.MetricEventBatch;
import org.okapi.wal.Wal.WalRecord;

/**
 * WalRecordAdapter for the Metrics event family.
 *
 * <p>Builds a WalRecord with the oneof body set to MetricEventBatch. The provided event list MUST
 * NOT be mutated by the adapter.
 */
public final class MetricWalRecordAdapter implements WalRecordAdapter<MetricEvent> {

  @Override
  public void validate(MetricEvent event) {
    checkNotNull(event, "metric event must not be null");
    // Add any domain-specific validation here if desired (e.g., ts.size == vals.size).
  }

  @Override
  public WalRecord buildRecord(List<MetricEvent> events) {
    checkNotNull(events, "events must not be null");
    // Build the batch and wrap into WalRecord.oneof body.
    MetricEventBatch batch = MetricEventBatch.newBuilder().addAllEvents(events).build();

    return WalRecord.newBuilder().setEvent(batch).build();
  }

  /**
   * For metrics, the default strategy (build then measure) is typically OK since batching already
   * materializes the protobuf message we need to write. Override provided for explicitness.
   */
  @Override
  public int recordPayloadSize(List<MetricEvent> events) {
    checkNotNull(events, "events must not be null");
    MetricEventBatch batch = MetricEventBatch.newBuilder().addAllEvents(events).build();
    // Size of the WalRecord payload (without outer framing).
    return WalRecord.newBuilder().setEvent(batch).build().getSerializedSize();
  }

  public static MetricWalRecordAdapter create(){
    return new MetricWalRecordAdapter();
  }
}
