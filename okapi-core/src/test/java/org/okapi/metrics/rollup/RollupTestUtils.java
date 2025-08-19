package org.okapi.metrics.rollup;

import java.util.List;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.fixtures.ReadingGeneratorReduction;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.stats.Statistics;

public class RollupTestUtils {
  public static void assertTolerableError(
      List<Float> expected, List<Float> actual, AGG_TYPE aggType, RES_TYPE resType) {
    assert expected.size() == actual.size()
        : "Expected and actual lists must have the same size for operation: "
            + aggType
            + " and resolution: "
            + resType;
    for (int i = 0; i < expected.size(); i++) {
      assertTolerableError(expected.get(i), actual.get(i), aggType, resType);
    }
  }

  public static void assertTolerableError(
      float expected, float actual, AGG_TYPE aggType, RES_TYPE resType) {
    // apart from percentiles, all other are done with float diffs
    if (aggType == AGG_TYPE.P50
        || aggType == AGG_TYPE.P75
        || aggType == AGG_TYPE.P90
        || aggType == AGG_TYPE.P95
        || aggType == AGG_TYPE.P99) {
      // for percentiles, we allow a small error margin
      var err = Math.abs(expected - actual) / actual;
      assert err < 0.02 // 2% error margin
          : "Expected: "
              + expected
              + ", Actual: "
              + actual
              + " for operation: "
              + aggType
              + " and resolution: "
              + resType;
    } else {
      // for other aggregations, we allow a very small error margin
      assert Math.abs(expected - actual) < 0.0001
          : "Expected: "
              + expected
              + ", Actual: "
              + actual
              + " for operation: "
              + aggType
              + " and resolution: "
              + resType;
    }
  }

  public static Statistics getStats(
      RollupSeries rollupSeries, String series, long timestamp, RES_TYPE resType) {
    return switch (resType) {
      case SECONDLY -> rollupSeries.getSecondlyStatistics(series, timestamp);
      case MINUTELY -> rollupSeries.getMinutelyStatistics(series, timestamp);
      case HOURLY -> rollupSeries.getHourlyStatistics(series, timestamp);
      default -> throw new IllegalArgumentException("Unknown resolution type: " + resType);
    };
  }

  public static float getStats(Statistics stats, AGG_TYPE aggType) {
    return switch (aggType) {
      case MIN -> stats.min();
      case MAX -> stats.max();
      case SUM -> stats.getSum();
      case AVG -> stats.avg();
      case COUNT -> stats.getCount();
      case P50 -> stats.percentile(0.5); // 50th percentile
      case P75 -> stats.percentile(0.75); // 75th percentile
      case P90 -> stats.percentile(0.9); // 90th percentile
      case P95 -> stats.percentile(0.95); // 95th percentile
      case P99 -> stats.percentile(0.99); // 99th percentile
    };
  }

  public static long timeToMillis(long val, RES_TYPE resType) {
    return switch (resType) {
      case SECONDLY -> val * 1000L; // convert seconds to milliseconds
      case MINUTELY -> val * 60 * 1000L; // convert minutes to milliseconds
      case HOURLY -> val * 60 * 60 * 1000L; // convert hours to milliseconds
      case DAILY -> val * 24 * 60 * 60 * 1000L; // convert days to milliseconds
    };
  }

  public static ReadingGeneratorReduction applyReduction(
      ReadingGenerator reading, AGG_TYPE aggType, RES_TYPE resType) {
    return switch (aggType) {
      case MIN -> reading.minReduction(resType);
      case MAX -> reading.maxReduction(resType);
      case SUM -> reading.sumReduction(resType);
      case AVG -> reading.avgReduction(resType);
      case COUNT -> reading.countReduction(resType);
      case P50 -> reading.percentile(resType, 0.5); // 50th percentile
      case P75 -> reading.percentile(resType, 0.75); // 50th quantile
      case P90 -> reading.percentile(resType, 0.9); // 90th quantile
      case P95 -> reading.percentile(resType, 0.95); // 95th quantile
      case P99 -> reading.percentile(resType, 0.99); // 99th quantile
    };
  }
}
