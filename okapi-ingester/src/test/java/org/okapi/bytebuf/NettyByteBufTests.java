/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

public class NettyByteBufTests {

  @Test
  void testToString() {
    ByteBuf heapBuf = ByteBufAllocator.DEFAULT.heapBuffer(256);
    heapBuf.writeInt(20);
  }
}
