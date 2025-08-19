package org.okapi.wal;

/**
 * SnapshotContext communicates a safe deletion watermark to the WAL cleaner. All WAL records with
 * LSN <= persistedLSN are guaranteed to be captured in a durable snapshot and can be safely removed
 * (subject to policy).
 */
public interface SnapshotContext {
  long persistedLSN();
}
