package org.okapi.wal;

/**
 * Synchronous callback invoked after a WAL record has been written to the active segment.
 *
 * <h3>Semantics</h3>
 * - Fired after bytes are successfully written to the FileChannel (i.e., "written"),
 *   NOT after an explicit fsync/force(). Durability to disk depends on your fsync policy.
 * - Called in the WAL writer's write path (synchronously, in-order). The caller is responsible
 *   for ensuring this hook does not block the write path.
 * - The hook MUST be idempotent keyed by LSN: it may be re-invoked during recovery or retries.
 *
 * <h3>Constraints (IMPORTANT)</h3>
 * - Keep work lightweight (update in-memory indexes, enqueue to an async worker, etc.).
 * - DO NOT call back into the WAL writer from this hook (no re-entrancy).
 * - Any exception thrown will propagate to the caller of write(), after the record has already
 *   been written to the channel. Decide whether to allow propagation (stop intake) or catch/log
 *   inside your implementation.
 */
public interface WalCommitListener {
    void onWalCommit(WalCommitContext ctx);
}
