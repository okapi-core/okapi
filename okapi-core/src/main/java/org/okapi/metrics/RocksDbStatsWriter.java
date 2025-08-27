package org.okapi.metrics;

import static com.google.api.client.util.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.okapi.exceptions.ExceptionUtils;
import org.okapi.metrics.constants.ReaderIds;
import org.okapi.metrics.rocks.RocksDbWriter;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.rollup.WriteBackSettings;
import org.okapi.metrics.stats.Statistics;
import org.okapi.metrics.stats.StatisticsRestorer;

@Slf4j
public class RocksDbStatsWriter implements Closeable {

  SharedMessageBox<WriteBackRequest> writes;
  ReadWriteLock rocksCreatorLock;
  StatisticsRestorer<Statistics> restorer;
  Merger<Statistics> merger;
  // used for scheduling writes
  ScheduledExecutorService scheduledExecutorService;
  RocksStore rocksStore;
  PathRegistry pathRegistry;

  // cancelling existing writes
  ScheduledFuture<?> scheduledWritesFuture;

  public RocksDbStatsWriter(
      SharedMessageBox<WriteBackRequest> writes,
      StatisticsRestorer<Statistics> restorer,
      Merger<Statistics> merger,
      PathRegistry pathRegistry)
      throws IOException {
    this.writes = checkNotNull(writes);
    this.rocksCreatorLock = new ReentrantReadWriteLock();
    this.restorer = checkNotNull(restorer);
    this.merger = checkNotNull(merger);
    this.pathRegistry = checkNotNull(pathRegistry);
  }

  protected void setRocksStore(RocksStore store) {
    this.rocksStore = checkNotNull(store);
  }

  public void startWriting(
      ScheduledExecutorService scheduledExecutorService,
      RocksStore rocksStore,
      WriteBackSettings writeBackSettings) {
    log.info("Started writing to rocksDB.");
    Preconditions.checkNotNull(scheduledExecutorService);
    setRocksStore(rocksStore);
    Preconditions.checkNotNull(writeBackSettings);
    this.scheduledExecutorService = scheduledExecutorService;
    this.scheduledWritesFuture =
        this.scheduledExecutorService.scheduleAtFixedRate(
            this::once, 0, writeBackSettings.getHotWindow().toMillis(), TimeUnit.MILLISECONDS);
  }

  public void once() {
    while (!writes.isEmpty()) {
      var sink = new ArrayList<WriteBackRequest>();
      writes.drain(sink, ReaderIds.MSG_STATS_WRITER);
      var groups = ArrayListMultimap.<String, WriteBackRequest>create();

      for (var req : sink) {
        groups.put(req.getKey(), req);
      }

      log.info("Writing {} keys.", groups.size());
      for (var v : groups.values()) {
        var path = pathRegistry.rocksPath(v.getShard());
        RocksDbWriter writer;
        try {
          writer = rocksStore.rocksWriter(path);
        } catch (IOException e) {
          log.info("Failed to obtain writer due to {}", ExceptionUtils.debugFriendlyMsg(e));
          return;
        }
        try {
          var key = v.getKey().getBytes();
          var previous = writer.get(key);
          if (previous == null) {
            writer.put(key, v.getStatistics().serialize());
            continue;
          }
          var stats = restorer.deserialize(previous);
          var update = merger.merge(stats, v.getStatistics());
          writer.put(key, update.serialize());
        } catch (Exception e) {
          log.error("writing failed with exception {}", ExceptionUtils.debugFriendlyMsg(e));
        }
      }

      log.info("Wrote {} keys.", groups.size());
    }
  }

  @Override
  public void close() throws IOException {
    if (this.scheduledWritesFuture != null) {
      this.scheduledWritesFuture.cancel(false);
    }
  }
}
