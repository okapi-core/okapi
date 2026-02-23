package org.okapi.pages;

import java.util.Optional;
import org.okapi.wal.lsn.Lsn;

/**
 * Generic append-only page abstraction for time-series data.
 *
 * @param <I> ingestion record type
 */
public interface AppendOnlyPage<I, S, M, B> {
  /** Append one record into the page. */
  void append(I obj);

  /** Optional inclusive time range covered by the page; empty if the page has no data. */
  Optional<InclusiveRange> range();

  /** Whether the page has no remaining capacity. Checked post-append. */
  boolean isFull();

  boolean isEmpty();

  S snapshot();

  M getMetadata();

  B getPageBody();

  Lsn getMaxLsn();

  void updateLsn(Lsn lsn);
}
