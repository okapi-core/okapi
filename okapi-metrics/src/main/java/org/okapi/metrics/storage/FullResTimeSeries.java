package org.okapi.metrics.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Getter;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.annotations.ThreadSafe;
import org.okapi.metrics.storage.buffers.BufferFullException;
import org.okapi.metrics.storage.snapshots.TimeSeriesSnapshot;
import org.okapi.metrics.storage.timediff.TimeDiffBuffer;
import org.okapi.metrics.storage.timediff.TimeDiffBufferSnapshot;
import org.okapi.metrics.storage.xor.XorBuffer;
import org.okapi.metrics.storage.xor.XorBufferSnapshot;

/**
 * This is an implementation of a full resolution time series that stores timestamps and values
 * using compressed buffers. It's not used in production, but it is kept here since implementing it
 * first time was a such a giant pain. Should the need arise again, we can use this implementation.
 * Its quite performant but consumes twice the memory of a rolled up buffer.
 */
@ThreadSafe
public class FullResTimeSeries {

  @Getter String shardId;
  @Getter int total;
  List<TimeDiffBuffer> timestamps;
  List<XorBuffer> values;
  BufferAllocator bufferAllocator;
  ReadWriteLock rwLock;
  int tsBufferSize;
  int valBufferSize;

  public FullResTimeSeries(
      String shardId, BufferAllocator allocator, int tsBufferSize, int valBufferSize) {
    this.shardId = shardId;
    this.bufferAllocator = allocator;
    this.rwLock = new ReentrantReadWriteLock();
    this.timestamps = new ArrayList<>();
    this.tsBufferSize = tsBufferSize;
    addTsBuffer();
    this.valBufferSize = valBufferSize;
    this.values = new ArrayList<>();
    addValBuffer();
  }

  private FullResTimeSeries(
      String shardId,
      int total,
      BufferAllocator allocator,
      List<TimeDiffBuffer> timeDiffBuffers,
      List<XorBuffer> xorBuffers,
      int tsBufferSize,
      int valBufferSize) {
    this.shardId = shardId;
    this.total = total;
    this.bufferAllocator = allocator;
    this.rwLock = new ReentrantReadWriteLock();
    this.timestamps = timeDiffBuffers;
    this.values = xorBuffers;
    this.tsBufferSize = tsBufferSize;
    this.valBufferSize = valBufferSize;
  }

  public static FullResTimeSeries restore(InputStream is, BufferAllocator bufferAllocator)
      throws StreamReadingException, IOException {
    OkapiIo.checkMagicNumber(is, TimeSeriesSnapshot.MAGIC);
    var nShardIdBytes = OkapiIo.readInt(is);
    var shardBytes = new byte[nShardIdBytes];
    for (int i = 0; i < nShardIdBytes; i++) {
      shardBytes[i] = (byte) is.read();
    }
    var total = OkapiIo.readInt(is);
    var nTimeBuffers = OkapiIo.readInt(is);
    var timeBufSize = OkapiIo.readInt(is);
    var timeDiffs = new ArrayList<TimeDiffBuffer>();
    for (int i = 0; i < nTimeBuffers; i++) {
      timeDiffs.add(TimeDiffBuffer.initialize(is, bufferAllocator.allocate(timeBufSize)));
    }

    var nValBufs = OkapiIo.readInt(is);
    var valBufSize = OkapiIo.readInt(is);
    var valBufs = new ArrayList<XorBuffer>();
    for (int i = 0; i < nValBufs; i++) {
      valBufs.add(XorBuffer.initialize(is, bufferAllocator.allocate(valBufSize)));
    }
    OkapiIo.checkMagicNumber(is, TimeSeriesSnapshot.MAGIC_END);
    return new FullResTimeSeries(
        new String(shardBytes),
        total,
        bufferAllocator,
        timeDiffs,
        valBufs,
        timeBufSize,
        valBufSize);
  }

  public void put(long ts, float val) throws CouldNotWrite {
    rwLock.writeLock().lock();
    try {
      appendTs(ts);
      appendVal(val);
      total++;
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  public TimeSeriesSnapshot snapshot() {
    rwLock.readLock().lock();
    try {
      var timeBuffs = new ArrayList<TimeDiffBufferSnapshot>();
      var valueBuffs = new ArrayList<XorBufferSnapshot>();
      for (var buf : timestamps) {
        timeBuffs.add(buf.snapshot());
      }

      for (var buf : values) {
        valueBuffs.add(buf.snapshot());
      }
      return new TimeSeriesSnapshot(
          timeBuffs, valueBuffs, total, tsBufferSize, valBufferSize, shardId);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  private AppendResult appendTsAux(long ts, int trial) throws CouldNotWrite {
    if (trial == 4) {
      throw new CouldNotWrite();
    }
    try {
      var lastBuffer = timestamps.getLast();
      lastBuffer.push(ts);
      return new AppendResult(null, true);
    } catch (BufferFullException e) {
      return new AppendResult(e, false);
    }
  }

  private AppendResult appendValAux(float val, int trial) throws CouldNotWrite {
    if (trial == 4) {
      throw new CouldNotWrite();
    }
    try {
      var lastBuffer = values.getLast();
      lastBuffer.push(val);
      return new AppendResult(null, true);
    } catch (BufferFullException e) {
      return new AppendResult(e, false);
    }
  }

  private void appendTs(long ts) throws CouldNotWrite {
    for (int trial = 1; trial <= 4; trial++) {
      var appendResult = appendTsAux(ts, trial);

      if (appendResult.ok()) {
        break;
      } else if (appendResult.e() instanceof BufferFullException) {
        addTsBuffer();
      }
    }
  }

  private void appendVal(float val) throws CouldNotWrite {
    for (int trial = 1; trial <= 4; trial++) {
      var result = appendValAux(val, trial);
      if (result.ok()) {
        break;
      } else if (result.e() instanceof BufferFullException) {
        addValBuffer();
      }
    }
  }

  private void addTsBuffer() {
    // allocate a  50 MB buffer by default
    var buffer = bufferAllocator.allocate(tsBufferSize);
    // gorilla buffer is a wrapper around a byte buffer
    timestamps.add(new TimeDiffBuffer(buffer));
  }

  private void addValBuffer() {
    // allocated a 50 MB buffer by default
    var buffer = bufferAllocator.allocate(valBufferSize);
    var writer = new ByteBufferWriter(buffer);
    values.add(new XorBuffer(writer));
  }

  record AppendResult(Exception e, boolean ok) {}
}
