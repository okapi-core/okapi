package org.okapi.metrics.rollup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.okapi.metrics.rollup.RollupTestUtils.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.okapi.fixtures.Percentiles;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.stats.*;

public class RolledupStatisticsTest {
  
  Supplier<RollupSeries<Statistics>> seriesSupplier;
  StatisticsRestorer<Statistics> statsRestorer;
  Supplier<Statistics> statisticsSupplier;
  RollupSeriesRestorer<Statistics> restorer;
  @BeforeEach
  public void setupSeries(){
    statsRestorer= new RolledupStatsRestorer();
    statisticsSupplier = new KllStatSupplier();
    restorer = new RolledUpSeriesRestorer(statsRestorer, statisticsSupplier);
    seriesSupplier = () -> new RollupSeries<>(statsRestorer, statisticsSupplier);
  }

  @Test
  public void testPercentile(){
    var arr = Arrays.asList(1.f, 2.f, 3.f, 5.f);
    System.out.println(Percentiles.getPercentile(arr, 0.9));
  }

  @ParameterizedTest
  // min, max, avg, quantile reductions,
  // minutely, hourly, daily metrics
  @ValueSource(ints = {1, 5, 10, 15, 30, 60, 120})
  public void testRollupCalculatesCorrectStats(int mins) {
    var ctx = new MetricsContext("test");
    var rollupStatistics = seriesSupplier.get();
    var reading = new ReadingGenerator(Duration.of(10, ChronoUnit.MILLIS), mins);
    reading.populateRandom(500.f, 540.f);
    var ts = reading.getTimestamps();
    var vals = reading.getValues();
    for (int i = 0; i < ts.size(); i++) {
      rollupStatistics.write(ctx, "SeriesA", ts.get(i), vals.get(i));
    }
    // apply reductions
    for (var reduction : AGG_TYPE.values()) {
      for (var resType : Arrays.asList(RES_TYPE.SECONDLY, RES_TYPE.MINUTELY, RES_TYPE.HOURLY)) {
        var reduced = applyReduction(reading, reduction, resType);
        for (int i = 0; i < reduced.getValues().size(); i++) {
          var expected = reduced.getValues().get(i);
          var time = reduced.getTimestamp().get(i);
          var val = getStats(rollupStatistics, "SeriesA", time, resType);
          var actual = getStats(val, reduction);
          assertTolerableError(expected, actual, reduction, resType);
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5, 10, 15, 30, 60, 120})
  public void testScanQueryResult(int mins) {
    var ctx = new MetricsContext("test");
    var rollupStatistics = seriesSupplier.get();
    var reading = new ReadingGenerator(Duration.of(10, ChronoUnit.MILLIS), mins);
    reading.populateRandom(500.f, 540.f);
    var ts = reading.getTimestamps();
    var vals = reading.getValues();
    for (int i = 0; i < ts.size(); i++) {
      rollupStatistics.write(ctx, "SeriesA", ts.get(i), vals.get(i));
    }
    // apply reductions
    for (var reduction : AGG_TYPE.values()) {
      for (var resType : Arrays.asList(RES_TYPE.SECONDLY, RES_TYPE.MINUTELY, RES_TYPE.HOURLY)) {
        var reduced = applyReduction(reading, reduction, resType);
        var scanResult =
            rollupStatistics.scan(
                "SeriesA",
                reduced.getTimestamp().getFirst(),
                reduced.getTimestamp().getLast(),
                reduction,
                resType);
        assertEquals(
            reduced.getTimestamp(),
            scanResult.getTimestamps(),
            "Timestamps should match for operation: " + reduction + " and resolution: " + resType);
        assertTolerableError(reduced.getValues(), scanResult.getValues(), reduction, resType);
      }
    }
  }
}
