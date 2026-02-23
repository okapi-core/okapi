/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import lombok.Getter;
import org.okapi.streams.StreamIdentifier;

final class PendingFlush<P, Id> {
  @Getter private final P page;
  @Getter private final StreamIdentifier<Id> streamIdentifier;

  PendingFlush(StreamIdentifier<Id> streamIdentifier, P page) {
    this.streamIdentifier = streamIdentifier;
    this.page = page;
  }
}
