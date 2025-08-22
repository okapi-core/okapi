package org.okapi.metrics.rollup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.collections.OkapiLists;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.query.QueryRecords;
import org.okapi.metrics.stats.*;

public class RollupQueryProcessorTests {
  // resources to create a series
  Supplier<RollupSeries<Statistics>> seriesSupplier;
  StatisticsRestorer<Statistics> statsRestorer;
  Supplier<Statistics> statisticsSupplier;
  RollupSeriesRestorer<Statistics> restorer;

  ReadingGenerator generator;
  ReadingGenerator generator2;
  RollupSeries<Statistics> rollupSeries;
  RollupQueryProcessor queryProcessor;
  long series1Start;
  long series2Start;
  long series1End;
  long series2End;
  MetricsContext ctx;

  @BeforeEach
  public void setUp() throws OutsideWindowException {
    ctx = new MetricsContext("test");
    generator = new ReadingGenerator(Duration.of(10, ChronoUnit.MILLIS), 20);
    generator.populateRandom(100.f, 110.f);
    statsRestorer = new RolledupStatsRestorer();
    statisticsSupplier = new KllStatSupplier();
    restorer = new RolledUpSeriesRestorer(statsRestorer, statisticsSupplier);
    seriesSupplier = () -> new RollupSeries<>(statsRestorer, statisticsSupplier);
    rollupSeries = seriesSupplier.get();
    rollupSeries.writeBatch(
        ctx,
        "test_series",
        OkapiLists.toLongArray(generator.getTimestamps()),
        OkapiLists.toFloatArray(generator.getValues()));
    series1Start = generator.getTimestamps().getFirst();
    series1End = generator.getTimestamps().getLast();

    generator2 = new ReadingGenerator(Duration.of(30, ChronoUnit.MILLIS), 20);
    generator2.populateRandom(100.f, 110.f);
    rollupSeries.writeBatch(
        ctx,
        "test_series_2",
        OkapiLists.toLongArray(generator2.getTimestamps()),
        OkapiLists.toFloatArray(generator2.getValues()));
    series2Start = generator2.getTimestamps().getFirst();
    series2End = generator2.getTimestamps().getLast();

    queryProcessor = new RollupQueryProcessor();
  }

  @Test
  public void testScanQuery() {
    var slice =
        new QueryRecords.Slice(
            "test_series", series1Start, series1End, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var reduction = generator.avgReduction(RES_TYPE.SECONDLY);
    var scanResult = queryProcessor.scan(rollupSeries, slice);
    assertEquals(reduction.getTimestamp(), scanResult.timestamps());
    assertEquals(reduction.getValues(), scanResult.values());
  }

  @Test
  public void testScale() {
    var slice =
        new QueryRecords.Slice(
            "test_series_2", series1Start, series1End, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var scaleFactor = 2.0f;
    var scaledResult = queryProcessor.scale(rollupSeries, slice, scaleFactor);
    var expectedValues = generator2.avgReduction(RES_TYPE.SECONDLY).getValues();
    for (int i = 0; i < expectedValues.size(); i++) {
      expectedValues.set(i, expectedValues.get(i) * scaleFactor);
    }
    assertEquals(
        generator2.avgReduction(RES_TYPE.SECONDLY).getTimestamp(), scaledResult.timestamps());
  }

  @Test
  public void testSum() {
    var slice1 =
        new QueryRecords.Slice(
            "test_series", series1Start, series1End, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var slice2 =
        new QueryRecords.Slice(
            "test_series_2", series2Start, series2End, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var sumResult = queryProcessor.sum(rollupSeries, slice1, slice2);
    // slice1 and slice2 have different timestamps, so we need to merge them
    // convert these to a map, then sum up values in the same timestamp
    var expectedTimestamps = generator.avgReduction(RES_TYPE.SECONDLY).getTimestamp();
    var expectedValues = generator.avgReduction(RES_TYPE.SECONDLY).getValues();
    var mapOfValues = new TreeMap<Long, Float>();
    for (int i = 0; i < expectedTimestamps.size(); i++) {
      mapOfValues.put(expectedTimestamps.get(i), expectedValues.get(i));
    }
    var secondTimestamps = generator2.avgReduction(RES_TYPE.SECONDLY).getTimestamp();
    var secondValues = generator2.avgReduction(RES_TYPE.SECONDLY).getValues();
    for (int i = 0; i < secondTimestamps.size(); i++) {
      mapOfValues.merge(
          secondTimestamps.get(i), secondValues.get(i), Float::sum); // sum values with same ts
    }

    var summedTimestamps = new ArrayList<>(mapOfValues.keySet());
    var summedValues = new ArrayList<Float>(mapOfValues.values());
    assertEquals(summedTimestamps, sumResult.timestamps());
    assertEquals(summedValues, sumResult.values());
  }

  @Test
  public void testSumSanity() throws OutsideWindowException {
    var time = System.currentTimeMillis();
    var ts1 = Arrays.asList(time, time + 1000, time + 2000);
    var vals1 = Arrays.asList(1.0f, 2.0f, 3.0f);

    var ts2 = Arrays.asList(time, time + 1000, time + 3000);
    var vals2 = Arrays.asList(4.0f, 5.0f, 6.0f);
    var rollupSeries = seriesSupplier.get();
    rollupSeries.writeBatch(
        ctx, "test_series_1", OkapiLists.toLongArray(ts1), OkapiLists.toFloatArray(vals1));
    rollupSeries.writeBatch(
        ctx, "test_series_2", OkapiLists.toLongArray(ts2), OkapiLists.toFloatArray(vals2));

    var queryProcessor = new RollupQueryProcessor();
    var slice1 =
        new QueryRecords.Slice("test_series_1", time, time + 2000, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var slice2 =
        new QueryRecords.Slice("test_series_2", time, time + 3000, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var sumResult = queryProcessor.sum(rollupSeries, slice1, slice2);
    var expectedTimestamps =
        Arrays.asList(
            roundToSeconds(time),
            roundToSeconds(time + 1000),
            roundToSeconds(time + 2000),
            roundToSeconds(time + 3000));
    var expectedValues = Arrays.asList(1.0f + 4.0f, 2.0f + 5.0f, 3.0f, 6.0f);
    assertEquals(expectedTimestamps, sumResult.timestamps());
    assertEquals(expectedValues, sumResult.values());
  }

  @Test
  public void testMovingWindowTransform() throws OutsideWindowException {
    var t0 = System.currentTimeMillis();
    var ts = Arrays.asList(t0, t0 + 2000, t0 + 3000, t0 + 5000);
    var values = Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f);
    var rollupSeries = seriesSupplier.get();
    rollupSeries.writeBatch(
        ctx, "test_series_1", OkapiLists.toLongArray(ts), OkapiLists.toFloatArray(values));
    var queryProcessor = new RollupQueryProcessor();
    var slice =
        new QueryRecords.Slice("test_series_1", t0, t0 + 6000, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var movingSum =
        queryProcessor.movingSum(rollupSeries, slice, Duration.of(2, ChronoUnit.SECONDS));
    var expectedTs =
        Stream.of(t0, t0 + 1000, t0 + 2000, t0 + 3000, t0 + 4000, t0 + 5000, t0 + 6000)
            .map(this::roundToSeconds)
            .toList();
    var expectedVals = Arrays.asList(1.0f, 1.0f, 3.0f, 5.0f, 5.0f, 7.0f, 4.0f);
    assertEquals(expectedTs, movingSum.timestamps());
    assertEquals(expectedVals, movingSum.values());
  }

  @Test
  public void testMovingWindowAverage() throws OutsideWindowException {
    var t0 = System.currentTimeMillis();
    var ts = Arrays.asList(t0, t0 + 2000, t0 + 3000, t0 + 5000);
    var values = Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f);
    var rollupSeries = seriesSupplier.get();
    rollupSeries.writeBatch(
        ctx, "test_series_1", OkapiLists.toLongArray(ts), OkapiLists.toFloatArray(values));
    var queryProcessor = new RollupQueryProcessor();
    var slice =
        new QueryRecords.Slice("test_series_1", t0, t0 + 6000, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var movingSum =
        queryProcessor.movingAverage(rollupSeries, slice, Duration.of(2, ChronoUnit.SECONDS));
    var expectedTs =
        Stream.of(t0, t0 + 1000, t0 + 2000, t0 + 3000, t0 + 4000, t0 + 5000, t0 + 6000)
            .map(this::roundToSeconds)
            .toList();
    var expectedVals = Arrays.asList(1.0f, 1.0f, 1.5f, 2.5f, 2.5f, 3.5f, 4.0f);
    assertEquals(expectedTs, movingSum.timestamps());
    assertEquals(expectedVals, movingSum.values());
  }

  public long roundToSeconds(long timestamp) {
    return 1000 * (timestamp / 1000);
  }
}
