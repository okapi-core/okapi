/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage.timediff;

import java.io.IOException;
import java.io.InputStream;
import lombok.Getter;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.storage.ByteBufferWriter;
import org.okapi.metrics.storage.buffers.AppendOnlyByteBuffer;
import org.okapi.metrics.storage.buffers.BufferFullException;

public class TimeDiffBuffer {
  int total;
  @Getter long first;
  @Getter long second;
  @Getter long previous;
  @Getter long beforePrevious;
  GorillaBuffer gorillaBuffer;

  public TimeDiffBuffer(AppendOnlyByteBuffer buffer) {
    this.gorillaBuffer = new GorillaBuffer(new ByteBufferWriter(buffer));
  }

  private TimeDiffBuffer() {}

  public static TimeDiffBuffer initialize(InputStream is, AppendOnlyByteBuffer buffer)
      throws StreamReadingException, IOException {
    OkapiIo.checkMagicNumber(is, TimeDiffBufferSnapshot.MAGIC_START);
    var total = OkapiIo.readInt(is);
    var first = OkapiIo.readLong(is);
    var second = OkapiIo.readLong(is);
    var beforePrevious = OkapiIo.readLong(is);
    var previous = OkapiIo.readLong(is);
    var gorillaSnap = GorillaBuffer.fromSnapshot(is, buffer);
    var restored = new TimeDiffBuffer();
    restored.previous = previous;
    restored.beforePrevious = beforePrevious;
    restored.total = total;
    restored.gorillaBuffer = gorillaSnap;
    restored.first = first;
    restored.second = second;
    OkapiIo.checkMagicNumber(is, TimeDiffBufferSnapshot.MAGIC_END);
    return restored;
  }

  protected GorillaBuffer getGorillaBuffer() {
    return gorillaBuffer;
  }

  public void push(long ts) throws BufferFullException {
    if (ts < 0) {
      throw new IllegalArgumentException("Timestamp cannot be negative");
    }
    if (total == 0) {
      first = ts; // fixed copy of first timestamp
      previous = ts;
      total++;
    } else if (total == 1) {
      second = ts; // fixed copy of second timestamp
      beforePrevious = previous;
      previous = ts;
      total++;
    } else {
      var diff1 = previous - beforePrevious;
      var diff2 = ts - previous;
      var diffOfDiff = (diff2 - diff1);
      if (diffOfDiff >= Integer.MIN_VALUE && diffOfDiff <= Integer.MAX_VALUE) {
        gorillaBuffer.writeInteger((int) diffOfDiff);
        beforePrevious = previous;
        previous = ts;
        total++;
      } else {
        throw new IllegalArgumentException("Too much jitter, value will not be written");
      }
    }
  }

  public TimeDiffBufferSnapshot snapshot() {
    // O(1) snapshot + contention time
    return new TimeDiffBufferSnapshot(
        first, second, beforePrevious, previous, total, gorillaBuffer.snapshot());
  }
}
