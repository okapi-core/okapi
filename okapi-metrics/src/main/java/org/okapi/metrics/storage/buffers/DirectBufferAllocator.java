/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage.buffers;

import java.nio.ByteBuffer;
import org.okapi.metrics.storage.BufferAllocator;

public class DirectBufferAllocator implements BufferAllocator {
  @Override
  public AppendOnlyByteBuffer allocate(int capacity) {
    return new AppendOnlyByteBuffer(ByteBuffer.allocateDirect(capacity), capacity);
  }

  @Override
  public AppendOnlyByteBuffer allocate(int requested, AppendOnlyByteBuffer contents) {
    var newBuffer = ByteBuffer.allocateDirect(requested);
    newBuffer.put(contents.getByteBuffer());
    return new AppendOnlyByteBuffer(newBuffer, requested);
  }
}
