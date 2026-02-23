package org.okapi.metrics.storage;

import org.okapi.metrics.storage.buffers.AppendOnlyByteBuffer;

public interface BufferAllocator {
  AppendOnlyByteBuffer allocate(int capacity);

  AppendOnlyByteBuffer allocate(int capacity, AppendOnlyByteBuffer contents);
}
