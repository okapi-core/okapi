package org.okapi.metrics.cas;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriFunction;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.ExceptionUtils;
import org.okapi.metrics.cas.dao.SearchHintDao;
import org.okapi.metrics.cas.dao.SketchesDao;
import org.okapi.metrics.cas.dao.TypeHintsDao;
import org.okapi.metrics.cas.dto.*;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.fdb.MetricTypesSearchVals;
import org.okapi.metrics.fdb.tuples.BUCKET_TYPE;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.service.runnables.MetricsWriter;
import org.okapi.metrics.service.validations.ValidateSubmitMetrics;
import org.okapi.metrics.stats.HistoStats;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.metrics.stats.UpdatableStatistics;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.payloads.Gauge;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.Sum;

@Slf4j
@AllArgsConstructor
public class CasMetricsWriter implements MetricsWriter {
  public static final Long MINUTE_BLOCK_LEN = Duration.of(1, ChronoUnit.MINUTES).toMillis();
  public static final Integer CONCURRENT_WRITES = 100000;
  public static final Integer SHARD_0 = 0;

  Supplier<UpdatableStatistics> statisticsSupplier;
  SketchesDao sketchesDao;
  SearchHintDao searchHintDao;
  TypeHintsDao typeHintsDao;
  ExecutorService executorService;

  @Override
  public void onRequestArrive(ExportMetricsRequest request)
      throws BadRequestException, StatisticsFrozenException, InterruptedException {
    ValidateSubmitMetrics.checkSubmitMetricsRequest(request);
    handleRequest(request);
    handleSearch(request);
    handleTypeHints(request);
  }

  protected void handleRequest(ExportMetricsRequest request) throws StatisticsFrozenException {
    switch (request.getType()) {
      case GAUGE -> handleGaugeRequest(request);
      case HISTO -> handleHistoRequest(request);
      case COUNTER -> handleSumsRequest(request);
    }
  }

  protected void handleTypeHints(ExportMetricsRequest request) {
    var localPath = MetricPaths.localPath(request.getMetricName(), request.getTags());
    var tenant = request.getTenantId();
    String metricType =
        switch (request.getType()) {
          case GAUGE -> org.okapi.metrics.fdb.MetricTypesSearchVals.GAUGE;
          case HISTO -> org.okapi.metrics.fdb.MetricTypesSearchVals.HISTO;
          case COUNTER ->
              switch (request.getSum().getSumType()) {
                case CUMULATIVE -> org.okapi.metrics.fdb.MetricTypesSearchVals.CSUM;
                case DELTA -> org.okapi.metrics.fdb.MetricTypesSearchVals.SUM;
              };
        };

    var hint =
        TypeHints.builder().tenantId(tenant).localPath(localPath).metricType(metricType).build();
    typeHintsDao.insert(hint);
  }

  protected void handleGaugeRequest(ExportMetricsRequest request) throws StatisticsFrozenException {
    var secondlyGroups = groupGauges(Collections.singletonList(request), this::casSecondlyBucket);
    var minutelyGroups = groupGauges(Collections.singletonList(request), this::casMinutelyBucket);
    var hourlyGroups = groupGauges(Collections.singletonList(request), this::casHourlyBucket);
    var paths = MetricPaths.localPath(request.getMetricName(), request.getTags());
    var secondlySketches =
        secondlyGroups.entrySet().stream()
            .map(
                entry ->
                    GaugeSketchSecondly.builder()
                        .tenantId(request.getTenantId())
                        .localPath(paths)
                        .sketch(ByteBuffer.wrap(entry.getValue().serialize()))
                        .secondBlock(entry.getKey().bucket())
                        .tenantId(entry.getKey().tenantId)
                        .build());
    var minutelySketches =
        minutelyGroups.entrySet().stream()
            .map(
                entry ->
                    GaugeSketchMinutely.builder()
                        .tenantId(request.getTenantId())
                        .localPath(paths)
                        .sketch(ByteBuffer.wrap(entry.getValue().serialize()))
                        .minuteBlock(entry.getKey().bucket())
                        .tenantId(entry.getKey().tenantId)
                        .build());
    var hrlySketches =
        hourlyGroups.entrySet().stream()
            .map(
                entry ->
                    GaugeSketchHourly.builder()
                        .tenantId(request.getTenantId())
                        .localPath(paths)
                        .sketch(ByteBuffer.wrap(entry.getValue().serialize()))
                        .hrBlock(entry.getKey().bucket())
                        .tenantId(entry.getKey().tenantId)
                        .build());
    var limiter = new Semaphore(CONCURRENT_WRITES);
    var secondlyFutures =
        secondlySketches
            .map(
                sk ->
                    executorService.submit(
                        new ThrottledFn(
                            limiter,
                            () -> {
                              sketchesDao.saveSecondlySketch(sk);
                            })))
            .toList();
    var minutelyFutures =
        minutelySketches
            .map(
                sk ->
                    executorService.submit(
                        new ThrottledFn(
                            limiter,
                            () -> {
                              sketchesDao.saveMinutelySketch(sk);
                            })))
            .toList();
    var hourlyFutures =
        hrlySketches
            .map(
                sk ->
                    executorService.submit(
                        new ThrottledFn(
                            limiter,
                            () -> {
                              sketchesDao.saveHourlySketch(sk);
                            })))
            .toList();
    for (var f : secondlyFutures) {
      waitForWriting(f);
    }
    for (var f : minutelyFutures) {
      waitForWriting(f);
    }
    for (var f : hourlyFutures) {
      waitForWriting(f);
    }
  }

  protected void waitForWriting(Future<?> f) {
    try {
      f.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.error("Writing interrupted before finish {}", ExceptionUtils.debugFriendlyMsg(e));
    } catch (TimeoutException e) {
      log.error("Could not finish writing all data withing 10 seconds");
    } catch (ExecutionException e) {
      log.error(
          "Could not finish writing due to execution exception {}",
          ExceptionUtils.debugFriendlyMsg(e));
    }
  }

  record GroupKey(String tenantId, String localPath, BUCKET_TYPE bucketType, long bucket) {}

  protected GroupKey casHourlyBucket(String tenant, String path, long ts) {
    return new GroupKey(tenant, path, BUCKET_TYPE.H, (ts / 1000 / 3600));
  }

  protected GroupKey casMinutelyBucket(String tenant, String path, long ts) {
    return new GroupKey(tenant, path, BUCKET_TYPE.M, (ts / 1000 / 60));
  }

  protected GroupKey casSecondlyBucket(String tenant, String path, long ts) {
    return new GroupKey(tenant, path, BUCKET_TYPE.S, (ts / 1000));
  }

  protected Map<GroupKey, UpdatableStatistics> groupGauges(
      List<ExportMetricsRequest> requests, TriFunction<String, String, Long, GroupKey> groupFn)
      throws StatisticsFrozenException {
    var group = new HashMap<GroupKey, UpdatableStatistics>();
    for (var r : requests) {
      var tenant = r.getTenantId();
      var path = MetricPaths.localPath(r.getMetricName(), r.getTags());
      var ctx = MetricsContext.createContext(r.getTenantId());
      var ts = r.getGauge().getTs();
      var vals = r.getGauge().getValue();
      for (int i = 0; i < ts.length; i++) {
        var g = groupFn.apply(tenant, path, ts[i]);
        var gStat = group.computeIfAbsent(g, (k) -> this.statisticsSupplier.get());
        gStat.update(ctx, vals[i]);
        group.put(g, gStat);
      }
    }
    return group;
  }

  protected void handleHistoRequest(ExportMetricsRequest request) throws StatisticsFrozenException {
    var histo = Preconditions.checkNotNull(request.getHisto());
    var pts = Preconditions.checkNotNull(histo.getHistoPoints());
    var path = MetricPaths.localPath(request.getMetricName(), request.getTags());
    // for delta -> record start / end

    for (var pt : pts) {
      var st = pt.getStart() / 1000; // secondly bucket
      var end = pt.getEnd() / 1000;
      var ubs = pt.getBuckets();
      var counts = pt.getBucketCounts();
      var histoSketch = new HistoStats(ubs, counts);
      var sketch =
          HistoSketch.builder()
              .tenantId(request.getTenantId())
              .localPath(path)
              .startSecond(st)
              .endSecond(end)
              .sketch(ByteBuffer.wrap(histoSketch.serialize()))
              .build();
      sketchesDao.saveHistoSketch(sketch);
    }
  }

  protected void handleSumsRequest(ExportMetricsRequest request) {
    var sum = Preconditions.checkNotNull(request.getSum());
    var pts = Preconditions.checkNotNull(sum.getSumPoints());
    var path = MetricPaths.localPath(request.getMetricName(), request.getTags());
    // for delta -> record start / end
    // for csum -> do set

    for (var pt : pts) {
      var st = pt.getStart() / 1000; // secondly bucket
      var end = pt.getEnd() / 1000;
      var value = Ints.toByteArray(pt.getSum());
      var count =
          CounterSketch.builder()
              .tenantId(request.getTenantId())
              .localPath(path)
              .startSecond(st)
              .endSecond(end)
              .sketch(ByteBuffer.wrap(value))
              .build();

      sketchesDao.saveCountSketch(count);
    }
  }

  public void handleSearch(ExportMetricsRequest request) throws InterruptedException {

    var path = MetricPaths.localPath(request.getMetricName(), request.getTags());
    var type =
        switch (request.getType()) {
          case COUNTER ->
              switch (request.getSum().getSumType()) {
                case CUMULATIVE -> MetricTypesSearchVals.CSUM;
                case DELTA -> MetricTypesSearchVals.SUM;
              };
          case HISTO -> MetricTypesSearchVals.HISTO;
          case GAUGE -> MetricTypesSearchVals.GAUGE;
        };

    Collection<Long> minuteBlocks = getMinutelyBlocks(request);
    for (var min : minuteBlocks) {
      var searchHint =
          SearchHints.builder()
              .tenantId(request.getTenantId())
              .shardKey(SHARD_0)
              .startMinute(min)
              .metricType(type)
              .localPath(path)
              .build();
      searchHintDao.insert(searchHint);
    }
  }

  protected Collection<Long> getMinutelyBlocks(ExportMetricsRequest request) {
    return switch (request.getType()) {
      case GAUGE -> getSearchBlocksForGauge(request.getGauge());
      case COUNTER -> getSearchBlocksForCounter(request.getSum());
      case HISTO -> getSearchBlocksForHisto(request.getHisto());
    };
  }

  protected Collection<Long> getSearchBlocksForGauge(Gauge gauge) {
    var blocks = new HashSet<Long>();
    for (var ts : gauge.getTs()) {
      blocks.add(ts / MINUTE_BLOCK_LEN);
    }
    return blocks;
  }

  protected Collection<Long> getSearchBlocksForCounter(Sum sum) {
    var blocks = new HashSet<Long>();
    for (var pt : sum.getSumPoints()) {
      var st = pt.getStart() / MINUTE_BLOCK_LEN;
      var en = pt.getEnd() / MINUTE_BLOCK_LEN;
      LongStream.range(st, en).forEach(blocks::add);
    }
    return blocks;
  }

  protected Collection<Long> getSearchBlocksForHisto(Histo histo) {
    var blocks = new HashSet<Long>();
    for (var pt : histo.getHistoPoints()) {
      var st = pt.getStart() / MINUTE_BLOCK_LEN;
      var en = pt.getEnd() / MINUTE_BLOCK_LEN;
      LongStream.range(st, en).forEach(blocks::add);
    }
    return blocks;
  }

  @Override
  public void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner) {}

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void init() throws IOException, StreamReadingException {}
}
