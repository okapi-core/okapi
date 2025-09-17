package org.okapi.fixtures;

import com.google.common.base.Preconditions;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BinaryOperator;
import lombok.Getter;
import org.okapi.metrics.pojos.RES_TYPE;

public class GaugeGenerator {
  @Getter List<Float> values = new ArrayList<>();
  @Getter List<Long> timestamps = new ArrayList<>();
  @Getter Duration tickerInterval;
  @Getter int minutes;
  @Getter Map<Long, List<Float>> groups;
  double gapFraction;

  public static final Duration ONE_MIN = Duration.of(1, ChronoUnit.MINUTES);
  public static final Duration ONE_HUNDRED_MILLIS = Duration.of(100, ChronoUnit.MILLIS);

  public GaugeGenerator(Duration tickerInterval, int minutes) {
    this.tickerInterval = tickerInterval;
    this.minutes = minutes;
  }

  public GaugeGenerator(Duration tickerInterval, int minutes, double gapFraction) {
    // values will be dropped with gapFraction
    this.tickerInterval = tickerInterval;
    this.minutes = minutes;
    Preconditions.checkArgument(
        gapFraction >= 0 && gapFraction <= 1,
        "Gap fraction must be between 0 and 1, got: " + gapFraction);
    this.gapFraction = gapFraction;
  }

  public static GaugeGenerator create(int minutes, float min, float max, double gapFraction) {
    var generator = new GaugeGenerator(ONE_HUNDRED_MILLIS, minutes, gapFraction);
    generator.populateRandom(min, max);
    return generator;
  }

  private void checkEmpty() {
    Preconditions.checkArgument(
        values.isEmpty(), "Values list is not empty, this fixture is not reusable");
    Preconditions.checkArgument(
        timestamps.isEmpty(), "Timestamps list is not empty, this fixture is not reusable");
  }

  public GaugeGenerator populateSame(Float val) {
    checkEmpty();
    var metricsPerMin = getMetricsPerMin();
    var now = timeToNearestMin();
    for (int i = 0; i < minutes; i++) {
      var atMinute = now + (i * ONE_MIN.toMillis());
      for (int j = 0; j < metricsPerMin; j++) {
        values.add(val);
        timestamps.add(atMinute);
      }
    }
    return this;
  }

  public long getMetricsPerMin() {
    return ONE_MIN.toMillis() / this.tickerInterval.toMillis();
  }

  public long timeToNearestMin() {
    return LocalDateTime.now()
        .truncatedTo(ChronoUnit.MINUTES)
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli();
  }

  public GaugeGenerator populateRandom(float min, float max) {
    var now = System.currentTimeMillis();
    var totalMetrics =
        Duration.of(minutes, ChronoUnit.MINUTES).toMillis() / tickerInterval.toMillis();
    var random = new Random();
    for (int i = 0; i < totalMetrics; i++) {
      if (random.nextDouble() < this.gapFraction) {
        continue;
      }
      var val = min + random.nextFloat() * (max - min);
      var ts = now + i * tickerInterval.toMillis();
      values.add(val);
      timestamps.add(ts);
    }
    return this;
  }

  public GaugeGenerator groupMetrics(RES_TYPE res) {
    groups = new TreeMap<>();
    for (int i = 0; i < timestamps.size(); i++) {
      var key = groupKey(timestamps.get(i), res);
      groups.putIfAbsent(key, new ArrayList<>());
      groups.get(key).add(values.get(i));
    }
    return this;
  }

  public ReadingAggregation reduce(
      float identity, BinaryOperator<Float> reduction, RES_TYPE resType) {
    groupMetrics(resType);
    var ts = new ArrayList<Long>();
    var values = new ArrayList<Float>();
    for (var entries : groups.entrySet()) {
      ts.add(groupKeyToTimestamp(entries.getKey(), resType)); // the times are in seconds
      values.add(entries.getValue().stream().reduce(identity, reduction));
    }
    return new ReadingAggregation(this, ts, values);
  }

  public ReadingAggregation minReduction(RES_TYPE resType) {
    return reduce(Float.MAX_VALUE, Float::min, resType);
  }

  public ReadingAggregation maxReduction(RES_TYPE resType) {
    return reduce(Float.MIN_VALUE, Float::max, resType);
  }

  public ReadingAggregation sumReduction(RES_TYPE resType) {
    return reduce(0.f, Float::sum, resType);
  }

  public ReadingAggregation countReduction(RES_TYPE resType) {
    return reduce(0.f, (a, b) -> a + 1, resType); // count is just the number of values
  }

  public ReadingAggregation percentile(RES_TYPE resType, double percentile) {
    groupMetrics(resType);
    var ts = new ArrayList<Long>();
    var values = new ArrayList<Float>();
    for (var entries : groups.entrySet()) {
      ts.add(groupKeyToTimestamp(entries.getKey(), resType)); // the times are in seconds
      var value = Percentiles.getPercentile(entries.getValue(), percentile);
      values.add(value);
    }
    return new ReadingAggregation(this, ts, values);
  }

  public ReadingAggregation avgReduction(RES_TYPE resType) {
    groupMetrics(resType);
    var ts = new ArrayList<Long>();
    var values = new ArrayList<Float>();
    for (var entries : groups.entrySet()) {
      ts.add(groupKeyToTimestamp(entries.getKey(), resType)); // the times are in seconds
      var avg = entries.getValue().stream().reduce(0.f, Float::sum) / (entries.getValue().size());
      values.add(avg);
    }
    return new ReadingAggregation(this, ts, values);
  }

  public Long groupKey(long ts, RES_TYPE res) {
    switch (res) {
      case SECONDLY:
        return ts / 1000; // convert to minutes
      case MINUTELY:
        return ts / 1000 / 60; // convert to hours
      case HOURLY:
        return ts / 1000 / 3600; // convert to days
      default:
        throw new IllegalArgumentException("Unsupported resolution: " + res);
    }
  }

  public Long groupKeyToTimestamp(long key, RES_TYPE res) {
    switch (res) {
      case SECONDLY:
        return key * 1000; // convert to milliseconds
      case MINUTELY:
        return key * 1000 * 60; // convert to milliseconds
      case HOURLY:
        return key * 1000 * 3600; // convert to milliseconds
      default:
        throw new IllegalArgumentException("Unsupported resolution: " + res);
    }
  }
}
