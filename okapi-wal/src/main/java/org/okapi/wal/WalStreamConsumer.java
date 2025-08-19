// CHANGE in: src/main/java/org/okapi/wal/WalStreamConsumer.java

package org.okapi.wal;

import org.okapi.wal.Wal.WalRecord;

/**
 * Consumer used by WalStreamer to apply pending WAL records during startup/recovery.
 * Implementations MUST be idempotent by LSN: if {@code lsn <= lastAppliedLsn()}, they should no-op.
 */
public interface WalStreamConsumer {
  /**
   * Apply one WAL record. Called in increasing LSN order with LSN > lastAppliedLsn().
   *
   * @param lsn global, monotonically increasing log sequence number
   * @param record parsed WalRecord payload
   * @throws Exception to abort streaming (caller will stop replay)
   */
  void consume(long lsn, WalRecord record) throws Exception;

  /**
   * The highest LSN that has been fully and durably applied by this consumer. WalStreamer will
   * stream records with LSN strictly greater than this value.
   */
  long lastAppliedLsn();

  /**
   * Optional: give the consumer a chance to flush/sync at the end of streaming. Default is a no-op.
   */
  default void flush() throws Exception {}
}
