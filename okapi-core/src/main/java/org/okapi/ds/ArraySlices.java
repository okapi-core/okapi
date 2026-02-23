/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.ds;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

public class ArraySlices {
  public static byte[] slice(byte[] src, int off, int len) {
    byte[] sliced = new byte[len];
    System.arraycopy(src, off, sliced, 0, len);
    return sliced;
  }

  public static byte[] concat(byte[] A, byte[] B) {
    byte[] concatenated = new byte[A.length + B.length];
    System.arraycopy(A, 0, concatenated, 0, A.length);
    System.arraycopy(B, 0, concatenated, A.length, B.length);
    return concatenated;
  }

  public static int readInt(byte[] buf, int off) {
    return Ints.fromBytes(buf[off], buf[off + 1], buf[off + 2], buf[off + 3]);
  }

  public static long readLong(byte[] buf, int off) {
    return Longs.fromBytes(
        buf[off], buf[off + 1],
        buf[off + 2], buf[off + 3],
        buf[off + 4], buf[off + 5],
        buf[off + 6], buf[off + 7]);
  }
}
