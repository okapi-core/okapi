/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage;

public interface BitWriter {
  void writeBit(boolean bit);

  boolean canWrite(int bits);
}
