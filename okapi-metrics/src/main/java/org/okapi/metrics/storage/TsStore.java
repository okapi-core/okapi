package org.okapi.metrics.storage;

import org.okapi.metrics.io.OkapiIo;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.storage.snapshots.TimeSeriesSnapshot;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TsStore {

  public static final Duration RETENTION = Duration.of(24, ChronoUnit.HOURS);
  Map<String, FullResTimeSeries> tsMap;
  Lock tsMapModifier;
  BufferAllocator bufferAllocator;
  int timeBufferSize;
  int valBufferSize;

  public void checkpoint(Path filePath) throws IOException {
    try (var fos = new FileOutputStream(filePath.toFile())) {
      OkapiIo.writeInt(fos, tsMap.size());
      OkapiIo.writeInt(fos, timeBufferSize);
      OkapiIo.writeInt(fos, valBufferSize);
      var keys = new ArrayList<>(tsMap.keySet());
      for (var key : keys) {
        var snap = tsMap.get(key).snapshot();
        // sequentially laid out shards
        snap.write(fos);
      }
      fos.getChannel().force(true);
    }
  }

  public TsStore(BufferAllocator allocator, int timeBufferSize, int valBufferSize) {
    this.bufferAllocator = allocator;
    this.timeBufferSize = timeBufferSize;
    this.valBufferSize = valBufferSize;
    this.tsMap = new ConcurrentHashMap<>();
    this.tsMapModifier = new ReentrantLock();
  }

  private TsStore(
      BufferAllocator allocator,
      int timeBufferSize,
      int valBufferSize,
      Map<String, FullResTimeSeries> tsStore) {
    this.bufferAllocator = allocator;
    this.timeBufferSize = timeBufferSize;
    this.valBufferSize = valBufferSize;
    this.tsMap = tsStore;
    this.tsMapModifier = new ReentrantLock();
  }

  public record WriteResult(Exception e, int written) {}

  public RangeScanResult shardsInRange(String series, long from, long to) {
    // which shards are in range
    // get a snapshot of all keys, check which ones are in range of `from` and `to`.
    // return a snapshot of the shards which overlap the range
    var hourStart = TimeUtils.roundDownToHour(from) / 2;
    var hourEnd = TimeUtils.roundUpToHour(to) / 2 + 1;
    // map hour start to the correct shard
    var snapshots = new ArrayList<TimeSeriesSnapshot>();
    for (long i = hourStart; i <= hourEnd; i++) {
      var shardId = getShardId(series, i);
      if (!tsMap.containsKey(shardId)) {
        continue;
      }
      var snapshot = tsMap.get(shardId).snapshot();
      snapshots.add(snapshot);
    }
    return new RangeScanResult(snapshots);
  }

  public List<String> checkPointableShards() {
    // given current system time, which shards are closed checkpointable.
    // returns upto `limit` number of shards
    var allKeys = new HashSet<>(tsMap.keySet());
    var checkpointable = new ArrayList<String>();
    var checkpointBucket =
        Long.toString((System.currentTimeMillis() - RETENTION.toMillis()) / 1000 / (2 * 3600));
    for (var key : allKeys) {
      if (key.endsWith(checkpointBucket)) {
        checkpointable.add(key);
      }
    }
    return checkpointable;
  }

  public void markCheckPointed(List<String> shards) {
    tsMapModifier.lock();
    try {
      for (var shard : shards) {
        tsMap.remove(shard);
      }
    } finally {
      tsMapModifier.unlock();
    }
  }

  public static TsStore restore(Path filePath, BufferAllocator bufferAllocator)
      throws IOException, StreamReadingException {
    // on a graceful shutdown and restart cycle, tsStore will be recreated using
    // tsStore.restore(checkpointPath).
    // restore should succeed before events consumption continues -> this is important to receive
    // software updates
    try (var fis = new FileInputStream(filePath.toFile())) {
      var nShards = OkapiIo.readInt(fis);
      var timeBufferSize = OkapiIo.readInt(fis);
      var valBufferSize = OkapiIo.readInt(fis);
      var tsMap = new HashMap<String, FullResTimeSeries>();
      for (int i = 0; i < nShards; i++) {
        var ts = FullResTimeSeries.restore(fis, bufferAllocator);
        tsMap.put(ts.getShardId(), ts);
      }
      return new TsStore(bufferAllocator, timeBufferSize, valBufferSize, tsMap);
    }
  }

  public WriteResult write(String series, long time, float val) {
    // hash the pts -> key = hash(series, time);
    // convert time to 2h window ->
    createIfRequired(series);
    var ts = tsMap.get(series);
    try {
      ts.put(time, val);
      return new WriteResult(null, 1);
    } catch (CouldNotWrite e) {
      return new WriteResult(e, 0);
    }
  }

  public static String getShardId(String series, long ts) {
    var _2hBlock = ts / 1000 / 7200;
    return series + ":" + _2hBlock;
  }

  public Optional<TimeSeriesSnapshot> snapshot(String shardId) {
    if (tsMap.containsKey(shardId)) {
      return Optional.ofNullable(tsMap.get(shardId).snapshot());
    }
    return Optional.empty();
  }

  // query methods: range() -> tsA, tsB
  // private methods
  private void createIfRequired(String series) {
    if (tsMap.containsKey(series)) {
      return;
    }
    tsMapModifier.lock();
    try {
      if (tsMap.containsKey(series)) {
        return;
      }
      tsMap.put(
          series, new FullResTimeSeries(series, bufferAllocator, timeBufferSize, valBufferSize));
    } finally {
      tsMapModifier.unlock();
    }
  }
}
