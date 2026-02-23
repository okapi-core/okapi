/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage;

public interface BitReader {
  boolean nextBit();

  boolean hasNext();
}
