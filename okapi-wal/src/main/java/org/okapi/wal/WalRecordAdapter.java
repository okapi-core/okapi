package org.okapi.wal;

import java.util.List;
import org.okapi.wal.Wal.WalRecord; // adjust if your generated package differs

/**
 * Builds a WalRecord for a specific event family (metrics, logs, traces...).
 * One WAL instance should use a single adapter (one family).
 */
public interface WalRecordAdapter<E> {

    /**
     * Build a WalRecord whose oneof body is set for this event family (e.g., MetricEventBatch).
     * The provided list MUST NOT be mutated by the adapter.
     */
    WalRecord buildRecord(List<E> events);

    /**
     * Optional fast-path size estimator for the WalRecord payload.
     * Default implementation builds the record and returns getSerializedSize().
     */
    default int recordPayloadSize(List<E> events) {
        return buildRecord(events).getSerializedSize();
    }

    /**
     * Optional per-event validation to enforce "one WAL == one event family" policy.
     * Default is no-op.
     */
    default void validate(E event) {}
}
