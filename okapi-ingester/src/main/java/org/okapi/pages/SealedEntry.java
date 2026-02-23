package org.okapi.pages;

import lombok.Setter;

final class SealedEntry<P> {
  final P page;
  @Setter volatile long persistedAt = 0L;

  SealedEntry(P page) {
    this.page = page;
  }
}
