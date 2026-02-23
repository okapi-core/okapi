/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics;

import org.okapi.metrics.storage.buffers.BufferSnapshot;

public class ByteDecoder {

  public static String decode(byte b) {
    var sb = new StringBuilder();
    for (int i = 7; i >= 0; i--) {
      if ((b & (1 << i)) > 0) sb.append('1');
      else sb.append('0');
    }

    return sb.toString();
  }

  public static String decode(BufferSnapshot snapshot) {
    var sb = new StringBuilder();
    for (int i = 0; i < snapshot.pos(); i++) {
      sb.append(decode(snapshot.appendOnlyByteBuffer().get(i)));
    }
    sb.append(decode(snapshot.partial()));

    var trim = snapshot.partial();
    sb.delete(sb.length() - trim, sb.length());
    return sb.toString();
  }
}
