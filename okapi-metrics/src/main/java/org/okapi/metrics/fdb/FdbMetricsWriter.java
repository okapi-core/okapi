package org.okapi.metrics.fdb;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.fdb.tuples.*;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.service.runnables.MetricsWriter;
import org.okapi.metrics.service.validations.ValidateSubmitMetrics;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.metrics.stats.UpdatableStatistics;
import org.okapi.rest.metrics.ExportMetricsRequest;

@AllArgsConstructor
public class FdbMetricsWriter implements MetricsWriter {
  String self;
  SharedMessageBox<FdbTx> messageBox;
  Supplier<UpdatableStatistics> statisticsSupplier;

  @Override
  public void onRequestArrive(ExportMetricsRequest request)
      throws BadRequestException, StatisticsFrozenException, InterruptedException {
    ValidateSubmitMetrics.checkSubmitMetricsRequest(request);
    handleRequest(request);
  }

  public void handleRequest(ExportMetricsRequest request)
      throws StatisticsFrozenException, InterruptedException {
    switch (request.getType()) {
      case COUNTER -> this.handleSumRequest(request);
      case HISTO -> this.handleHistoRequest(request);
    }
    handleSearch(request);
  }

  public void handleSearch(ExportMetricsRequest request) throws InterruptedException {
    switch (request.getType()) {
      case GAUGE -> handleSearchForGauge(request);
      case HISTO -> handleSearchForHisto(request);
      case COUNTER -> handleSearchForSum(request);
    }
  }

  public void handleSearchForGauge(ExportMetricsRequest exportMetricsRequest)
      throws InterruptedException {
    var buckets = new HashSet<Long>();
    var gauge = exportMetricsRequest.getGauge();
    for (var t : gauge.getTs()) {
      buckets.add(t / 60_000);
    }
    var searchPath =
        MetricPaths.localPath(exportMetricsRequest.getMetricName(), exportMetricsRequest.getTags());
    for (var bucket : buckets) {
      var searchTuple = new SearchTuple(exportMetricsRequest.getTenantId(), searchPath, bucket);
      this.messageBox.push(new FdbTx(searchTuple.pack(), MetricTypesSearchVals.GAUGE.getBytes()));
    }
  }

  public void handleSearchForHisto(ExportMetricsRequest exportMetricsRequest)
      throws InterruptedException {
    var buckets = new HashSet<Long>();
    var histo = exportMetricsRequest.getHisto();
    for (var pt : histo.getHistoPoints()) {
      var startBucket = pt.getStart() / 60_000;
      var endBucket = pt.getEnd() / 60_000;
      for (long i = startBucket; i <= endBucket; i++) {
        buckets.add(i);
      }
    }

    var searchPath =
        MetricPaths.localPath(exportMetricsRequest.getMetricName(), exportMetricsRequest.getTags());
    for (var bucket : buckets) {
      var searchTuple = new SearchTuple(exportMetricsRequest.getTenantId(), searchPath, bucket);
      this.messageBox.push(new FdbTx(searchTuple.pack(), MetricTypesSearchVals.HISTO.getBytes()));
    }
  }

  public void handleSearchForSum(ExportMetricsRequest request) throws InterruptedException {
    var buckets = new HashSet<Long>();
    var sum = request.getSum();
    var value =
        switch (sum.getSumType()) {
          case CUMULATIVE -> MetricTypesSearchVals.CSUM;
          case DELTA -> MetricTypesSearchVals.SUM;
        };
    var path = MetricPaths.localPath(request.getMetricName(), request.getTags());
    for (var pt : sum.getSumPoints()) {
      // add points for start / end of the sums -> these are supposed to be discrete.
      var bucketStart = pt.getStart() / 60_000;
      var bucketEnd = pt.getEnd() / 60_000;
      for (long i = bucketStart; i <= bucketEnd; i++) {
        buckets.add(i);
      }
    }

    for (var bucket : buckets) {
      var tuple = new SearchTuple(request.getTenantId(), path, bucket);
      this.messageBox.push(new FdbTx(tuple.pack(), value.getBytes()));
    }
  }


  public void handleSumRequest(ExportMetricsRequest request) throws InterruptedException {
    var path = MetricPaths.convertToPath(request);
    var sum = Preconditions.checkNotNull(request.getSum());
    var sumType = sum.getSumType();
    var pts = Preconditions.checkNotNull(sum.getSumPoints());
    // for delta -> record start / end
    // for csum -> do set

    for (var pt : pts) {
      var st = pt.getStart() / 1000; // secondly bucket
      var end = pt.getEnd() / 1000;
      var value = Ints.toByteArray(pt.getSum());
      var key =
          switch (sumType) {
            case DELTA -> new DeltaSumTuple(path, self, st, end);
            case CUMULATIVE -> new CsumTuple(path, st, end);
          };

      this.messageBox.push(new FdbTx(key.pack(), value));
    }
  }

  public void handleHistoRequest(ExportMetricsRequest request) throws InterruptedException {
    var path = MetricPaths.convertToPath(request);
    var histo = Preconditions.checkNotNull(request.getHisto());
    var pts = Preconditions.checkNotNull(histo.getHistoPoints());
    for (var pt : pts) {
      var st = pt.getStart() / 1000;
      var end = pt.getEnd() / 1000;
      var counts = pt.getBucketCounts();
      var buckets = pt.getBuckets();
      for (int i = 0; i < buckets.length; i++) {
        var histoTuple = new HistoTuple(path, buckets[i], false, st, end, self);
        var count = counts[i];
        this.messageBox.push(new FdbTx(histoTuple.pack(), Ints.toByteArray(count)));
      }
      var histoTuple = new HistoTuple(path, null, true, st, end, self);
      var value = counts[buckets.length];
      this.messageBox.push(new FdbTx(histoTuple.pack(), Ints.toByteArray(value)));
    }
  }

  @Override
  public void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner) {
    // no concept of shards or series
    return;
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void init() throws IOException, StreamReadingException {}
}
