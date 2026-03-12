/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.datagen.spans;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.metrics.v1.*;
import io.opentelemetry.proto.resource.v1.Resource;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.okapi.collections.OkapiLists;
import org.okapi.datagen.spans.MetricsDataGenConfig.DistributionSpec;
import org.okapi.datagen.spans.MetricsDataGenConfig.MetricSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MetricsDataGenerator {
  private final MetricsDataGenConfig config;

  public MetricsDataGenerator(MetricsDataGenConfig config) {
    this.config = config;
  }

  public List<ExportMetricsServiceRequest> generate() {
    long endMs = System.currentTimeMillis();
    long startMs = endMs - config.getTimeWindowMs();
    var out = new ArrayList<ExportMetricsServiceRequest>();

    out.add(buildMetrics(config.getHostMetrics(), startMs, endMs));
    out.add(buildMetrics(config.getKafkaMetrics(), startMs, endMs));
    out.add(buildMetrics(config.getSpanMetrics(), startMs, endMs));

    return out;
  }

  private ExportMetricsServiceRequest buildMetrics(
      List<MetricSpec> specs, long startMs, long endMs) {
    var metrics = new ArrayList<Metric>();
    for (MetricSpec spec : specs) {
      metrics.add(buildMetric(spec, startMs, endMs));
    }
    return wrapMetrics(metrics);
  }

  private Metric buildMetric(MetricSpec spec, long startMs, long endMs) {
    var builder = Metric.newBuilder().setName(spec.getName());
    if (spec.getUnit() != null && !spec.getUnit().isEmpty()) {
      builder.setUnit(spec.getUnit());
    }
    return switch (spec.getType()) {
      case GAUGE -> builder.setGauge(buildGauge(spec, startMs, endMs)).build();
      case SUM -> builder.setSum(buildSum(spec, startMs, endMs)).build();
      case HISTO -> builder.setHistogram(buildHistogram(spec, startMs, endMs)).build();
    };
  }

  private Gauge buildGauge(MetricSpec spec, long startMs, long endMs) {
    long intervalMs = gaugeIntervalMs();
    var points = new ArrayList<NumberDataPoint>();
    var sampler = samplerFor(spec.getDistribution());
    for (long ts = startMs; ts <= endMs; ts += intervalMs) {
      points.add(numberPoint(ts, sampler.sample(), spec.getTags()));
    }
    return Gauge.newBuilder().addAllDataPoints(points).build();
  }

  private Sum buildSum(MetricSpec spec, long startMs, long endMs) {
    long intervalMs = config.getSumHistogramIntervalMs();
    var points = new ArrayList<NumberDataPoint>();
    var sampler = samplerFor(spec.getDistribution());
    for (long intervalStart = startMs; intervalStart < endMs; intervalStart += intervalMs) {
      long intervalEnd = Math.min(intervalStart + intervalMs, endMs);
      points.add(sumPoint(intervalStart, intervalEnd, sampler.sample(), spec.getTags()));
    }
    return Sum.newBuilder()
        .setAggregationTemporality(AggregationTemporality.AGGREGATION_TEMPORALITY_DELTA)
        .setIsMonotonic(true)
        .addAllDataPoints(points)
        .build();
  }

  private Histogram buildHistogram(MetricSpec spec, long startMs, long endMs) {
    long intervalMs = config.getSumHistogramIntervalMs();
    var points = new ArrayList<HistogramDataPoint>();
    var bounds =
        spec.getHistogramBounds().isEmpty()
            ? config.getDefaultHistogramBounds()
            : spec.getHistogramBounds();
    var rng = new MersenneTwister(config.getSeed() + spec.getName().hashCode());
    var dist =
        new LogNormalDistribution(
            rng, spec.getDistribution().getMu(), spec.getDistribution().getSigma());
    int countMean =
        spec.getHistogramCountMean() == null
            ? 50
            : Math.max(1, spec.getHistogramCountMean().intValue());

    for (long intervalStart = startMs; intervalStart < endMs; intervalStart += intervalMs) {
      long intervalEnd = Math.min(intervalStart + intervalMs, endMs);
      int sampleCount = new PoissonDistribution(rng, countMean, 1e-12, 100000).sample();
      points.add(
          histogramPoint(intervalStart, intervalEnd, bounds, dist, sampleCount, spec.getTags()));
    }
    return Histogram.newBuilder()
        .setAggregationTemporality(AggregationTemporality.AGGREGATION_TEMPORALITY_DELTA)
        .addAllDataPoints(points)
        .build();
  }

  private ExportMetricsServiceRequest wrapMetrics(List<Metric> metrics) {
    var scopeMetrics = ScopeMetrics.newBuilder().addAllMetrics(metrics).build();
    var resource = Resource.newBuilder().build();
    var resourceMetrics =
        ResourceMetrics.newBuilder().setResource(resource).addScopeMetrics(scopeMetrics).build();
    return ExportMetricsServiceRequest.newBuilder().addResourceMetrics(resourceMetrics).build();
  }

  private NumberDataPoint numberPoint(long tsMs, double value, Map<String, String> attrs) {
    return NumberDataPoint.newBuilder()
        .setTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(tsMs))
        .setAsDouble(value)
        .addAllAttributes(OtelShorthand.toKvList(attrs))
        .build();
  }

  private NumberDataPoint sumPoint(
      long startMs, long endMs, double value, Map<String, String> attrs) {
    return NumberDataPoint.newBuilder()
        .setStartTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(startMs))
        .setTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(endMs))
        .setAsDouble(value)
        .addAllAttributes(OtelShorthand.toKvList(attrs))
        .build();
  }

  private HistogramDataPoint histogramPoint(
      long startMs,
      long endMs,
      List<Double> bounds,
      LogNormalDistribution dist,
      int sampleCount,
      Map<String, String> attrs) {
    var counts = new long[bounds.size() + 1];
    double sum = 0.0;
    int safeCount = Math.max(0, sampleCount);
    for (int i = 0; i < safeCount; i++) {
      double value = dist.sample();
      sum += value;
      int bucket = bucketIndex(bounds, value);
      counts[bucket] += 1;
    }
    return HistogramDataPoint.newBuilder()
        .setStartTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(startMs))
        .setTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(endMs))
        .addAllExplicitBounds(bounds)
        .addAllBucketCounts(OkapiLists.toList(toBoxed(counts)))
        .setCount(safeCount)
        .setSum(sum)
        .addAllAttributes(OtelShorthand.toKvList(attrs))
        .build();
  }

  private int bucketIndex(List<Double> bounds, double value) {
    for (int i = 0; i < bounds.size(); i++) {
      if (value <= bounds.get(i)) {
        return i;
      }
    }
    return bounds.size();
  }

  private Long[] toBoxed(long[] values) {
    return java.util.Arrays.stream(values).boxed().toArray(Long[]::new);
  }

  private long gaugeIntervalMs() {
    if (config.getGaugeSamplingRate() <= 0) {
      return 1000L;
    }
    long intervalMs = Math.round(1000.0 / config.getGaugeSamplingRate());
    return Math.max(1L, intervalMs);
  }

  private Sampler samplerFor(DistributionSpec spec) {
    var rng = new MersenneTwister(System.currentTimeMillis());
    return switch (spec.getType()) {
      case BETA -> new Sampler(new BetaDistribution(rng, spec.getAlpha(), spec.getBeta()), spec);
      case LOG_NORMAL ->
          new Sampler(new LogNormalDistribution(rng, spec.getMu(), spec.getSigma()), spec);
      case POISSON ->
          new Sampler(new PoissonDistribution(rng, spec.getMean(), 1e-12, 100000), spec);
      case BERNOULLI -> new Sampler(spec, rng);
      case LOGNORMAL_X_POISSON ->
          new Sampler(
              new LogNormalDistribution(rng, spec.getMu(), spec.getSigma()),
              new PoissonDistribution(rng, spec.getMean(), 1e-12, 100000),
              spec);
    };
  }

  private double clamp(double value, DistributionSpec spec) {
    if (spec.getMin() != null) {
      value = Math.max(spec.getMin(), value);
    }
    if (spec.getMax() != null) {
      value = Math.min(spec.getMax(), value);
    }
    return value;
  }

  private class Sampler {
    private final DistributionSpec spec;
    private final BetaDistribution beta;
    private final LogNormalDistribution logNormal;
    private final PoissonDistribution poisson;
    private final MersenneTwister rng;

    Sampler(BetaDistribution beta, DistributionSpec spec) {
      this.beta = beta;
      this.logNormal = null;
      this.poisson = null;
      this.spec = spec;
      this.rng = null;
    }

    Sampler(LogNormalDistribution logNormal, DistributionSpec spec) {
      this.beta = null;
      this.logNormal = logNormal;
      this.poisson = null;
      this.spec = spec;
      this.rng = null;
    }

    Sampler(PoissonDistribution poisson, DistributionSpec spec) {
      this.beta = null;
      this.logNormal = null;
      this.poisson = poisson;
      this.spec = spec;
      this.rng = null;
    }

    Sampler(DistributionSpec spec, MersenneTwister rng) {
      this.beta = null;
      this.logNormal = null;
      this.poisson = null;
      this.spec = spec;
      this.rng = rng;
    }

    Sampler(LogNormalDistribution logNormal, PoissonDistribution poisson, DistributionSpec spec) {
      this.beta = null;
      this.logNormal = logNormal;
      this.poisson = poisson;
      this.spec = spec;
      this.rng = null;
    }

    double sample() {
      double value;
      switch (spec.getType()) {
        case BETA -> value = beta.sample();
        case LOG_NORMAL -> value = logNormal.sample();
        case POISSON -> value = poisson.sample();
        case BERNOULLI -> value = rng.nextDouble() <= spec.getP() ? 1.0 : 0.0;
        case LOGNORMAL_X_POISSON -> {
          value = logNormal.sample() * poisson.sample();
        }
        default -> value = 0.0;
      }
      if (spec.getScale() != null) {
        value *= spec.getScale();
      }
      return clamp(value, spec);
    }
  }
}
