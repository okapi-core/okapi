package org.okapi.fixtures;

import com.google.common.base.Preconditions;
import org.okapi.metrics.pojos.RES_TYPE;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BinaryOperator;
import lombok.Getter;

public class ReadingGenerator {
  @Getter List<Float> values = new ArrayList<>();
  @Getter List<Long> timestamps = new ArrayList<>();
  @Getter Duration tickerInterval;
  @Getter int minutes;
  @Getter Map<Long, List<Float>> groups;
  double gapFraction;

  public static final Duration ONE_MIN = Duration.of(1, ChronoUnit.MINUTES);
  public static final Duration ONE_HUNDRED_MILLIS = Duration.of(100, ChronoUnit.MILLIS);

  public ReadingGenerator(Duration tickerInterval, int minutes) {
    this.tickerInterval = tickerInterval;
    this.minutes = minutes;
  }

  public ReadingGenerator(Duration tickerInterval, int minutes, double gapFraction) {
    // values will be dropped with gapFraction
    this.tickerInterval = tickerInterval;
    this.minutes = minutes;
    Preconditions.checkArgument(
        gapFraction >= 0 && gapFraction <= 1,
        "Gap fraction must be between 0 and 1, got: " + gapFraction);
    this.gapFraction = gapFraction;
  }

  public static ReadingGenerator create(
      int minutes, float min, float max, double gapFraction) {
    var generator = new ReadingGenerator(ONE_HUNDRED_MILLIS, minutes, gapFraction);
    generator.populateRandom(min, max);
    return generator;
  }

  private void checkEmpty() {
    Preconditions.checkArgument(
        values.isEmpty(), "Values list is not empty, this fixture is not reusable");
    Preconditions.checkArgument(
        timestamps.isEmpty(), "Timestamps list is not empty, this fixture is not reusable");
  }

  public ReadingGenerator populateSame(Float val) {
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

  // should generate values with average = val, but not necessarily the same
  public ReadingGenerator populateWithAverage(float avg, float deviation) {
    checkEmpty();
    var metricsPerMin = getMetricsPerMin();
    var half = metricsPerMin / 2;
    var now = timeToNearestMin();
    var isOdd = 2 * half < metricsPerMin;

    var random = new Random();
    for (int i = 0; i < minutes; i++) {
      var atMinute = now + (i * tickerInterval.toMillis());
      for (int j = 0; j < half; j++) {
        var perturbation = random.nextFloat() * deviation;
        values.add(avg + perturbation);
        timestamps.add(atMinute);

        values.add(avg - perturbation);
        timestamps.add(atMinute);
      }

      if (isOdd) {
        values.add(avg);
        timestamps.add(atMinute);
      }
    }
    return this;
  }

  public ReadingGenerator populateWithMin(float min, float deviation) {
    checkEmpty();
    var now = timeToNearestMin();
    var random = new Random();
    for (int i = 0; i < minutes; i++) {
      var atMinute = now + (i * tickerInterval.toMillis());
      for (int j = 0; j < getMetricsPerMin(); j++) {
        var perturbation = (j == 0) ? 0 : random.nextFloat() * deviation;
        values.add(min + perturbation);
        timestamps.add(atMinute);
      }
    }
    return this;
  }

  public ReadingGenerator populateWithMax(float max, float deviation) {
    checkEmpty();
    var now = timeToNearestMin();

    var random = new Random();
    for (int i = 0; i < minutes; i++) {
      var atMinute = now + (i * tickerInterval.toMillis());
      for (int j = 0; j < getMetricsPerMin(); j++) {
        var perturbation = (j == 0) ? 0 : random.nextFloat() * deviation;
        values.add(max - perturbation);
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

  public ReadingGenerator populateRandom(float min, float max) {
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

  public ReadingGenerator groupMetrics(RES_TYPE res) {
    groups = new TreeMap<>();
    for (int i = 0; i < timestamps.size(); i++) {
      var key = groupKey(timestamps.get(i), res);
      groups.putIfAbsent(key, new ArrayList<>());
      groups.get(key).add(values.get(i));
    }
    return this;
  }

  public ReadingGeneratorReduction reduce(
      float identity, BinaryOperator<Float> reduction, RES_TYPE resType) {
    groupMetrics(resType);
    var ts = new ArrayList<Long>();
    var values = new ArrayList<Float>();
    for (var entries : groups.entrySet()) {
      ts.add(groupKeyToTimestamp(entries.getKey(), resType)); // the times are in seconds
      values.add(entries.getValue().stream().reduce(identity, reduction));
    }
    return new ReadingGeneratorReduction(this, ts, values);
  }

  public ReadingGeneratorReduction minReduction(RES_TYPE resType) {
    return reduce(Float.MAX_VALUE, Float::min, resType);
  }

  public ReadingGeneratorReduction maxReduction(RES_TYPE resType) {
    return reduce(Float.MIN_VALUE, Float::max, resType);
  }

  public ReadingGeneratorReduction sumReduction(RES_TYPE resType) {
    return reduce(0.f, Float::sum, resType);
  }

  public ReadingGeneratorReduction countReduction(RES_TYPE resType) {
    return reduce(0.f, (a, b) -> a + 1, resType); // count is just the number of values
  }

  public ReadingGeneratorReduction percentile(RES_TYPE resType, double percentile) {
    groupMetrics(resType);
    var ts = new ArrayList<Long>();
    var values = new ArrayList<Float>();
    for (var entries : groups.entrySet()) {
      ts.add(groupKeyToTimestamp(entries.getKey(), resType)); // the times are in seconds
      var value = Percentiles.getPercentile(entries.getValue(), percentile);
      values.add(value);
    }
    return new ReadingGeneratorReduction(this, ts, values);
  }

  public ReadingGeneratorReduction avgReduction(RES_TYPE resType) {
    groupMetrics(resType);
    var ts = new ArrayList<Long>();
    var values = new ArrayList<Float>();
    for (var entries : groups.entrySet()) {
      ts.add(groupKeyToTimestamp(entries.getKey(), resType)); // the times are in seconds
      var avg = entries.getValue().stream().reduce(0.f, Float::sum) / (entries.getValue().size());
      values.add(avg);
    }
    return new ReadingGeneratorReduction(this, ts, values);
  }

  public Long groupKey(long ts, RES_TYPE res) {
    switch (res) {
      case SECONDLY:
        return ts / 1000; // convert to minutes
      case MINUTELY:
        return ts / 1000 / 60; // convert to hours
      case HOURLY:
        return ts / 1000 / 3600; // convert to days
      case DAILY:
        return ts / 1000 / 3600 / 24; // convert to days
      default:
        throw new IllegalArgumentException("Unsupported resolution: " + res);
    }
  }

  private static long getIncrement(RES_TYPE resType) {
    return switch (resType) {
      case SECONDLY -> 1000L;
      case MINUTELY -> 60 * 1000L;
      case HOURLY -> 60 * 60 * 1000L;
      case DAILY -> 86400L * 1000;
    };
  }

  private static long discretizeAndBack(long ts, RES_TYPE resType) {
    var inc = getIncrement(resType);
    return inc * (ts / inc);
  }

  public Long groupKeyToTimestamp(long key, RES_TYPE res) {
    switch (res) {
      case SECONDLY:
        return key * 1000; // convert to milliseconds
      case MINUTELY:
        return key * 1000 * 60; // convert to milliseconds
      case HOURLY:
        return key * 1000 * 3600; // convert to milliseconds
      case DAILY:
        return key * 1000 * 3600 * 24; // convert to milliseconds
      default:
        throw new IllegalArgumentException("Unsupported resolution: " + res);
    }
  }
}
