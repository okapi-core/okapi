package org.okapi.metrics.storage.timediff;

import org.okapi.metrics.storage.BufferSnapshot;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.metrics.storage.snapshots.GorillaSnapshot;

import java.io.IOException;
import java.io.OutputStream;

public class TimeDiffBufferSnapshot implements BufferSnapshot<Long> {
  public static final String MAGIC_START = "TimeDiff";
  public static final String MAGIC_END = "TimeDiffEnd";
  int served = 0;
  int total;
  long previous;
  long beforePrevious;
  GorillaSnapshot gorillaSnapshot;

  // for snapshotting only
  long first;
  long second;
  long last;
  long beforeLast;

  public TimeDiffBufferSnapshot(
      long first, long second, long beforeLast, long last, int total, GorillaSnapshot gorillaSnapshot) {
    this.total = total;
    this.beforePrevious = first;
    this.previous = second;
    this.gorillaSnapshot = gorillaSnapshot;
    this.first = first;
    this.second = second;
    this.last = last;
    this.beforeLast  = beforeLast;
  }

  public int size(){
    return total;
  }

  @Override
  public boolean hasNext() {
    return served < total;
  }

  @Override
  public Long next() {
    if (served == 0) {
      served++;
      return beforePrevious;
    } else if (served == 1) {
      served++;
      return previous;
    } else {
      var diffOfDiff = this.gorillaSnapshot.next();
      var diff1 = previous - beforePrevious;
      var diff2 = diff1 + diffOfDiff;
      var ts = previous + diff2;
      beforePrevious = previous;
      previous = ts;
      served++;
      return ts;
    }
  }

  @Override
  public void write(OutputStream os) throws IOException {
    OkapiIo.writeString(os, MAGIC_START);
    OkapiIo.writeInt(os, total);
    OkapiIo.writeLong(os, first);
    OkapiIo.writeLong(os, second);
    OkapiIo.writeLong(os, beforeLast);
    OkapiIo.writeLong(os, last);
    gorillaSnapshot.write(os);
    OkapiIo.writeString(os, MAGIC_END);
  }
}
