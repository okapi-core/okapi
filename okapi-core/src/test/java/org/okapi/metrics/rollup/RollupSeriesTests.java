package org.okapi.metrics.rollup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.okapi.Statistics;
import org.okapi.collections.OkapiLists;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.stats.*;
import org.okapi.testutils.OkapiTestUtils;

@Execution(ExecutionMode.CONCURRENT)
public class RollupSeriesTests {

  StatisticsRestorer<Statistics> statsRestorer;
  Supplier<UpdatableStatistics> statisticsSupplier;
  RollupSeriesRestorer<UpdatableStatistics> restorer;
  Function<Integer, RollupSeries<UpdatableStatistics>> seriesFunction;

  @BeforeEach
  public void setupSeries() {
    seriesFunction = new RollupSeriesFn();
    statsRestorer = new ReadonlyRestorer();
    statisticsSupplier = new KllStatSupplier();
    restorer = new RolledUpSeriesRestorer(seriesFunction);
  }

  @Test
  public void testRollupSeries()
      throws IOException, StatisticsFrozenException, InterruptedException {
    var series = seriesFunction.apply(0);
    var ts = System.currentTimeMillis();
    var ctx = new MetricsContext("test");
    series.write(ctx, "series1", ts, 100.0f);
    series.write(ctx, "series1", ts, 100.0f);
    series.write(ctx, "series2", ts, 200.0f);
    var checkpointPath = Files.createTempFile("rollup", ".checkpoint");
    series.checkpoint(checkpointPath);
  }

  @Test
  public void testRollupSeriesWithMultipleWrites()
      throws IOException, StreamReadingException, StatisticsFrozenException, InterruptedException {
    var ctx = new MetricsContext("test");
    var ts = System.currentTimeMillis();
    var series = seriesFunction.apply(0);
    for (int i = 0; i < 10; i++) {
      series.write(ctx, "series1", ts + i, 100.0f + i);
      series.write(ctx, "series2", ts + i, 200.0f + i);
    }
    var checkpointPath = Files.createTempFile("rollup", ".checkpoint");
    series.checkpoint(checkpointPath);
    var seriesSupplier = new RollupSeriesFn();
    var restorer = new RolledUpSeriesRestorer(seriesSupplier);
    var restored = restorer.restore(0, checkpointPath);
    OkapiTestUtils.checkEquals(restored, series);
  }

  @ParameterizedTest
  @MethodSource("testRestoreFuzzyArgs")
  public void testRestoreFuzzy(Duration ticker, int minutes)
      throws IOException, StreamReadingException, StatisticsFrozenException, InterruptedException {
    var readings = new ReadingGenerator(ticker, minutes);
    var ctx = new MetricsContext("test");
    readings.populateRandom(500.f, 540.f);
    var ts = readings.getTimestamps();
    var vals = readings.getValues();
    var series = seriesFunction.apply(0);
    for (int i = 0; i < ts.size(); i++) {
      series.write(ctx, "series1", ts.get(i), vals.get(i));
    }

    var checkpointPath = Files.createTempFile("rollup", ".checkpoint");
    series.checkpoint(checkpointPath);
    var fileSize = Files.size(checkpointPath);
    assertTrue(fileSize > 0, "Checkpoint file should not be empty");
    var restored = restorer.restore(0, checkpointPath);
    OkapiTestUtils.checkEquals(series, restored);
  }

  public static Stream<Arguments> testRestoreFuzzyArgs() {
    return Stream.of(
        Arguments.of(Duration.of(10, ChronoUnit.MILLIS), 5),
        Arguments.of(Duration.of(10, ChronoUnit.MILLIS), 50),
        Arguments.of(Duration.of(20, ChronoUnit.MILLIS), 120),
        Arguments.of(Duration.of(20, ChronoUnit.MILLIS), 24 * 60),
        Arguments.of(Duration.of(10, ChronoUnit.MILLIS), 24 * 60));
  }

  public RollupSeries write(RollupSeries series, String timeSeries, List<Long> ts, List<Float> vals)
      throws OutsideWindowException, StatisticsFrozenException, InterruptedException {
    series.writeBatch(
        new MetricsContext("ctx"),
        timeSeries,
        OkapiLists.toLongArray(ts),
        OkapiLists.toFloatArray(vals));
    return series;
  }
}
