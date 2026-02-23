package org.okapi.metrics.storage.fakes;

import java.nio.ByteBuffer;
import lombok.Getter;
import org.okapi.metrics.storage.BufferAllocator;
import org.okapi.metrics.storage.buffers.AppendOnlyByteBuffer;

public class SharedBufferAllocator implements BufferAllocator {
  @Getter AppendOnlyByteBuffer buffer;

  @Override
  public AppendOnlyByteBuffer allocate(int capacity) {
    if (buffer == null) {
      buffer = new AppendOnlyByteBuffer(ByteBuffer.allocate(capacity), capacity);
    }
    return buffer;
  }

  @Override
  public AppendOnlyByteBuffer allocate(int capacity, AppendOnlyByteBuffer contents) {
    var newBuffer = new AppendOnlyByteBuffer(ByteBuffer.allocateDirect(capacity), capacity);
    for (int i = 0; i < buffer.pos(); i++) {
      newBuffer.put(buffer.get(i));
    }
    buffer = newBuffer;
    return buffer;
  }
}
