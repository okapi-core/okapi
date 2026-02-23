/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage.snapshots;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.okapi.io.OkapiIo;
import org.okapi.metrics.TimeSeriesBatchDecoder;
import org.okapi.metrics.storage.timediff.TimeDiffBufferSnapshot;
import org.okapi.metrics.storage.xor.XorBufferSnapshot;

// contains a decoded snapshot of a timestamp buffer
public class TimeSeriesSnapshot implements TimeSeriesBatchDecoder {

  public static final String MAGIC = "TimeSeries";
  public static final String MAGIC_END = "TimeSeriesEnd";
  final String shardId;
  // snapshots are O(1) time and storage since buffers are not copied around
  @Getter List<TimeDiffBufferSnapshot> timeDiffBuffers;
  @Getter List<XorBufferSnapshot> xors;
  int total;
  int tsBufferSize;
  int valBufferSize;
  int gBufIdx;
  int xorBufIdx;
  int g; // where are we in the respective buffers
  int v;
  int served = 0;

  public TimeSeriesSnapshot(
      List<TimeDiffBufferSnapshot> gorillaSnapshots,
      List<XorBufferSnapshot> xorBufferSnapshots,
      int total,
      int tsBufferSize,
      int valBufferSize,
      String shardId) {
    this.timeDiffBuffers = Collections.unmodifiableList(gorillaSnapshots);
    this.xors = Collections.unmodifiableList(xorBufferSnapshots);
    this.total = total;
    this.tsBufferSize = tsBufferSize;
    this.valBufferSize = valBufferSize;
    this.shardId = shardId;
  }

  public int size() {
    return total;
  }

  @Override
  public boolean hasMore() {
    return served < total;
  }

  @Override
  public int next(long[] times, float[] values) {
    int expected = values.length;
    int written = 0;
    while (gBufIdx < timeDiffBuffers.size()
        && g < timeDiffBuffers.get(gBufIdx).size()
        && xorBufIdx < xors.size()
        && v < xors.get(xorBufIdx).size()
        && written < expected) {
      var gBuf = timeDiffBuffers.get(gBufIdx);
      var xorBuf = xors.get(xorBufIdx);
      var time = gBuf.next();
      g++;
      var val = xorBuf.next();
      v++;

      values[written] = val;
      times[written] = time;
      written++;
      if (g == gBuf.size()) {
        g = 0;
        gBufIdx++;
      }
      if (v == xorBuf.size()) {
        v = 0;
        xorBufIdx++;
      }
    }
    served += written;
    return written;
  }

  public void write(OutputStream os) throws IOException {
    OkapiIo.writeString(os, MAGIC);
    var shardBytes = shardId.getBytes();
    OkapiIo.writeInt(os, shardBytes.length);
    os.write(shardBytes);
    OkapiIo.writeInt(os, total);
    OkapiIo.writeInt(os, timeDiffBuffers.size());
    OkapiIo.writeInt(os, tsBufferSize);
    for (var timeSnaps : timeDiffBuffers) {
      timeSnaps.write(os);
    }
    OkapiIo.writeInt(os, xors.size());
    OkapiIo.writeInt(os, valBufferSize);
    for (var xorSnaps : xors) {
      xorSnaps.write(os);
    }
    OkapiIo.writeString(os, MAGIC_END);
  }
}
