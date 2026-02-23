package org.okapi.metrics.storage.buffers;

import java.nio.ByteBuffer;
import org.okapi.metrics.storage.BufferAllocator;

public class HeapBufferAllocator implements BufferAllocator {
  @Override
  public AppendOnlyByteBuffer allocate(int capacity) {
    return new AppendOnlyByteBuffer(ByteBuffer.allocate(capacity), capacity);
  }

  @Override
  public AppendOnlyByteBuffer allocate(int capacity, AppendOnlyByteBuffer contents) {
    var newBuffer = ByteBuffer.allocate(capacity);
    newBuffer.put(contents.getByteBuffer());
    return new AppendOnlyByteBuffer(newBuffer, capacity);
  }
}
