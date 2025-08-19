package org.okapi.metrics.storage.buffers;

import org.okapi.metrics.storage.BufferAllocator;
import java.nio.ByteBuffer;

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
