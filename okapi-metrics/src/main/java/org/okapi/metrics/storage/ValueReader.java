/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage;

public interface ValueReader {
  int readInteger(int bits);

  int readUInt(int bits);

  boolean readBit();
  //    long readLong(int bits);
}
