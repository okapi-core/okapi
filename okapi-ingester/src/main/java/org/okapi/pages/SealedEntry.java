/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import lombok.Setter;

final class SealedEntry<P> {
  final P page;
  @Setter volatile long persistedAt = 0L;

  SealedEntry(P page) {
    this.page = page;
  }
}
