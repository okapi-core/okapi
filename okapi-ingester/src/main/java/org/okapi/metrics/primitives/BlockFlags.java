/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.primitives;

public class BlockFlags {
  public static final byte[] GAUGE = new byte[] {0x0, 0x0};
  public static final byte[] HISTO = new byte[] {0x0, 0x1};
}
