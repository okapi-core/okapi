package org.okapi.metrics.fdb;

import com.apple.foundationdb.Database;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.Merger;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.fdb.tuples.BUCKET_TYPE;
import org.okapi.metrics.fdb.tuples.MetricWriteTuple;
import org.okapi.metrics.fdb.tuples.SearchTuple;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.metrics.stats.StatisticsRestorer;
import org.okapi.metrics.stats.UpdatableStatistics;
import org.okapi.rest.metrics.SubmitMetricsRequestInternal;

@Slf4j
public class FdbWriter {
  public static final String FDB_CONSUMER = FdbWriter.class.getSimpleName();

  SharedMessageBox<SubmitMetricsRequestInternal> consumerBox;
  Merger<UpdatableStatistics> merger;
  Database db;
  Node self;
  Supplier<UpdatableStatistics> statisticsSupplier;
  StatisticsRestorer<UpdatableStatistics> unmarshaller;

  public FdbWriter(
      SharedMessageBox<SubmitMetricsRequestInternal> consumerBox,
      Merger<UpdatableStatistics> merger,
      Database db,
      Node self,
      Supplier<UpdatableStatistics> statisticsSupplier,
      StatisticsRestorer<UpdatableStatistics> unmarshaller) {
    this.consumerBox = consumerBox;
    this.merger = merger;
    this.db = db;
    this.self = self;
    this.statisticsSupplier = statisticsSupplier;
    this.unmarshaller = unmarshaller;
  }

  public void writeOnce() throws StatisticsFrozenException {
    log.debug("Consuming from box");
    while (!consumerBox.isEmpty()) {
      var sink = Lists.<SubmitMetricsRequestInternal>newArrayList();
      consumerBox.drain(sink, FDB_CONSUMER);
      // group by key -> secondly minutely hourly buckets
      var secondlyGroups = groupBy(sink, this::fdbSecondlyBucket);
      var minutelyGroups = groupBy(sink, this::fdbMinutelyBucket);
      var hourlyGroups = groupBy(sink, this::fdbHourlyBucket);
      writeStats(secondlyGroups);
      writeStats(minutelyGroups);
      writeStats(hourlyGroups);

      var searchKeys = groupForSearch(sink);
      writeSearchKeys(searchKeys);
    }
  }

  public void writeSearchKeys(Set<SearchTuple> keys) {
    var value = Longs.toByteArray(1L);
    db.run(
        tr -> {
          for (var k : keys) {
            tr.set(k.toKey(), value);
          }
          return null;
        });
  }

  public Set<SearchTuple> groupForSearch(List<SubmitMetricsRequestInternal> requestInternals) {
    var group = new HashSet<SearchTuple>();
    for (var r : requestInternals) {
      var path = MetricPaths.convertToPath(r);
      for (var ts : r.getTs()) {
        var minutely = ts / 60_000;
        var key = new SearchTuple(path, minutely);
        group.add(key);
      }
    }
    return group;
  }

  public void writeStats(Map<GroupKey, UpdatableStatistics> group) {
    db.run(
        tr -> {
          for (var entry : group.entrySet()) {
            var groupKey = entry.getKey();
            var tuple =
                new MetricWriteTuple(
                    groupKey.ts(), groupKey.bucketType(), groupKey.bucket(), this.self.id());
            tr.set(tuple.pack(), entry.getValue().serialize());
          }
          return null;
        });
  }

  public Map<GroupKey, UpdatableStatistics> groupBy(
      List<SubmitMetricsRequestInternal> requests, BiFunction<String, Long, GroupKey> groupFn)
      throws StatisticsFrozenException {
    var group = new HashMap<GroupKey, UpdatableStatistics>();
    for (var r : requests) {
      var path = MetricPaths.convertToPath(r);
      var ctx = MetricsContext.createContext(r.getTenantId());
      for (int i = 0; i < r.getTs().length; i++) {
        var g = groupFn.apply(path, r.getTs()[i]);
        var gStat = group.computeIfAbsent(g, (k) -> this.statisticsSupplier.get());
        gStat.update(ctx, r.getValues()[i]);
        group.put(g, gStat);
      }
    }
    return group;
  }

  record GroupKey(String ts, BUCKET_TYPE bucketType, long bucket) {}

  protected GroupKey fdbHourlyBucket(String timeSeries, long ts) {
    return new GroupKey(timeSeries, BUCKET_TYPE.H, (ts / 1000 / 3600));
  }

  protected GroupKey fdbMinutelyBucket(String timeSeries, long ts) {
    return new GroupKey(timeSeries, BUCKET_TYPE.M, (ts / 1000 / 60));
  }

  protected GroupKey fdbSecondlyBucket(String timeSeries, long ts) {
    return new GroupKey(timeSeries, BUCKET_TYPE.S, (ts / 1000));
  }
}
