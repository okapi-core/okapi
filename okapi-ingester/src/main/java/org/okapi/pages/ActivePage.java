/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;
import lombok.Locked;
import org.okapi.streams.StreamIdentifier;
import org.okapi.wal.lsn.Lsn;

/** Thread-safe wrapper around an AppendOnlyPage that handles rotation when full. */
public final class ActivePage<P extends AppendOnlyPage<I, S, M, B>, I, S, M, B, Id> {
  @Getter private final StreamIdentifier<Id> streamIdentifier;
  private final Function<StreamIdentifier<Id>, P> pageFactory;
  private P page;

  public ActivePage(
      StreamIdentifier<Id> streamIdentifier, Function<StreamIdentifier<Id>, P> pageFactory) {
    this.streamIdentifier = streamIdentifier;
    this.pageFactory = pageFactory;
    this.page = pageFactory.apply(streamIdentifier);
  }

  /** Append and rotate if page becomes full. Returns sealed page if rotated. */
  @Locked.Write
  public Optional<P> append(Lsn lsn, I record) {
    page.append(record);
    page.updateLsn(lsn);
    if (page.isFull()) {
      P sealed = page;
      page = pageFactory.apply(streamIdentifier);
      return Optional.of(sealed);
    }
    return Optional.empty();
  }

  /** Snapshot current active page (treat as read-only). */
  @Locked.Read
  public S snapshot() {
    return page.snapshot();
  }

  /** Rotate now if page has any data and is older than boundary by start timestamp. */
  @Locked.Write
  public Optional<P> rotateIfOlderThanAndNotEmpty(long boundaryTsMillis) {
    if (page.isEmpty()) return Optional.empty();
    var r = page.range();
    if (r.isPresent() && r.get().startInclusive() < boundaryTsMillis) {
      P sealed = page;
      page = pageFactory.apply(streamIdentifier);
      return Optional.of(sealed);
    }
    return Optional.empty();
  }

  /** Rotate unconditionally if page has any data. */
  @Locked.Write
  public Optional<P> rotateIfNonEmpty() {
    if (!page.isEmpty()) {
      P sealed = page;
      page = pageFactory.apply(streamIdentifier);
      return Optional.of(sealed);
    }
    return Optional.empty();
  }
}
