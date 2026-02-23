/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.query;

import java.util.concurrent.atomic.AtomicBoolean;

public class CancellationToken {
  private final AtomicBoolean cancelled = new AtomicBoolean(false);

  public void cancel() {
    cancelled.set(true);
  }

  public boolean isCancelled() {
    return cancelled.get();
  }
}
