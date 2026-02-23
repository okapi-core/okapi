package org.okapi.metrics.query;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.okapi.logs.query.QueryConfig;
import org.okapi.metrics.service.GaugeAggregator;
import org.okapi.metrics.service.HistoAggregator;
import org.okapi.metrics.service.MetricsQueryService;
import org.okapi.primitives.ReadonlyHistogram;
import org.okapi.primitives.TimestampedReadonlySketch;
import org.okapi.rest.metrics.query.GaugeQueryConfig;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.metrics.search.SearchMetricsRequestInternal;
import org.okapi.rest.metrics.search.SearchMetricsResponse;
import org.okapi.spring.configs.Qualifiers;
import org.springframework.beans.factory.annotation.Qualifier;

public class MultisourceMetricsQueryService implements MetricsQueryService {
  OnDiskMetricsQueryProcessor onDiskMetricsQueryProcessor;
  S3MetricsQp s3MetricsQp;
  PeerMetricsQp peerMetricsQp;
  ExecutorService executors;
  Duration queryTimeout;

  public MultisourceMetricsQueryService(
      OnDiskMetricsQueryProcessor onDiskMetricsQueryProcessor,
      S3MetricsQp s3MetricsQp,
      PeerMetricsQp peerMetricsQp,
      @Qualifier(Qualifiers.EXEC_METRICS_MULTI_SOURCE) ExecutorService executors,
      @Qualifier(Qualifiers.METRICS_PEER_QUERY_TIMEOUT) Duration queryTimeout) {
    this.onDiskMetricsQueryProcessor = onDiskMetricsQueryProcessor;
    this.s3MetricsQp = s3MetricsQp;
    this.peerMetricsQp = peerMetricsQp;
    this.executors = executors;
    this.queryTimeout = queryTimeout;
  }

  @Override
  public GetMetricsResponse getMetricsResponse(GetMetricsRequest request, QueryConfig queryConfig)
      throws Exception {
    if (request.getGaugeQueryConfig() != null) {
      return processGaugeQuery(request, queryConfig);
    } else {
      return processHistogramQuery(request, queryConfig);
    }
  }

  @Override
  public SearchMetricsResponse searchMetricsResponse(
      SearchMetricsRequestInternal searchMetricsRequest, QueryConfig config) {
    throw new UnsupportedOperationException();
  }

  public GetMetricsResponse processGaugeQuery(GetMetricsRequest request, QueryConfig queryConfig)
      throws Exception {
    var gaugeQueryConfig = request.getGaugeQueryConfig();
    var qps = new ArrayList<MetricsQueryProcessor>();
    if (queryConfig.disk) {
      qps.add(onDiskMetricsQueryProcessor);
    }
    if (queryConfig.s3) {
      qps.add(s3MetricsQp);
    }
    if (queryConfig.fanOut) {
      qps.add(peerMetricsQp);
    }
    var joiner = new ParrallelAggregator<TimestampedReadonlySketch>(executors);
    var suppliers = getSuppliers(request, qps, gaugeQueryConfig);
    var sketches = joiner.aggregate(suppliers, queryTimeout);
    var gaugeResponse =
        GaugeAggregator.aggregateSketches(
            sketches, gaugeQueryConfig.getResolution(), gaugeQueryConfig.getAggregation());
    return GetMetricsResponse.builder()
        .metric(request.getMetric())
        .tags(request.getTags())
        .gaugeResponse(gaugeResponse)
        .build();
  }

  @NotNull
  private static ArrayList<Supplier<List<ReadonlyHistogram>>> getHistoSuppliers(
      GetMetricsRequest request, ArrayList<MetricsQueryProcessor> qps) {
    var suppliers = new ArrayList<Supplier<List<ReadonlyHistogram>>>();
    for (var qp : qps) {
      Supplier<List<ReadonlyHistogram>> supplier =
          () -> {
            try {
              return qp.getHistograms(
                  request.getMetric(), request.getTags(), request.getStart(), request.getEnd());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          };
      suppliers.add(supplier);
    }
    return suppliers;
  }

  @NotNull
  private static ArrayList<Supplier<List<TimestampedReadonlySketch>>> getSuppliers(
      GetMetricsRequest request,
      ArrayList<MetricsQueryProcessor> qps,
      GaugeQueryConfig gaugeQueryConfig) {
    var suppliers = new ArrayList<Supplier<List<TimestampedReadonlySketch>>>();
    for (var qp : qps) {
      Supplier<List<TimestampedReadonlySketch>> supplier =
          () -> {
            try {
              return qp.getGaugeSketches(
                  request.getMetric(),
                  request.getTags(),
                  gaugeQueryConfig.getResolution(),
                  request.getStart(),
                  request.getEnd());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          };
      suppliers.add(supplier);
    }
    return suppliers;
  }

  public GetMetricsResponse processHistogramQuery(
      GetMetricsRequest request, QueryConfig queryConfig) throws Exception {
    var qps = new ArrayList<MetricsQueryProcessor>();
    if (queryConfig.disk) {
      qps.add(onDiskMetricsQueryProcessor);
    }
    if (queryConfig.s3) {
      qps.add(s3MetricsQp);
    }
    if (queryConfig.fanOut) {
      qps.add(peerMetricsQp);
    }
    var joiner = new ParrallelAggregator<ReadonlyHistogram>(executors);
    var suppliers = getHistoSuppliers(request, qps);
    var histograms = joiner.aggregate(suppliers, queryTimeout);
    var histogramResponse = HistoAggregator.aggregateHistograms(histograms);
    return GetMetricsResponse.builder()
        .metric(request.getMetric())
        .tags(request.getTags())
        .histogramResponse(histogramResponse)
        .build();
  }
}
