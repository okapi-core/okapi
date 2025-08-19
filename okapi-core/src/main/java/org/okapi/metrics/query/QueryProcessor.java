package org.okapi.metrics.query;

import org.okapi.metrics.rollup.RollupSeries;

import java.time.Duration;

public interface QueryProcessor {
  public enum TRANSFORM {
    LOG,
    SIGMOID
  }

  QueryRecords.QueryResult scan(RollupSeries rollupSeries, QueryRecords.Slice slice);

  QueryRecords.QueryResult scale(RollupSeries rollupSeries,QueryRecords.Slice slice, float scaleFactor);

  QueryRecords.QueryResult sum(RollupSeries rollupSeries,QueryRecords.Slice left, QueryRecords.Slice right);

  float count(RollupSeries rollupSeries,QueryRecords.Slice slice);

  QueryRecords.QueryResult transform(RollupSeries rollupSeries,QueryRecords.Slice slice, TRANSFORM transform);

  QueryRecords.QueryResult movingAverage(RollupSeries rollupSeries,QueryRecords.Slice slice, Duration windowSize);

  QueryRecords.QueryResult movingSum(RollupSeries rollupSeries,QueryRecords.Slice slice, Duration windowSize);

  QueryRecords.QueryResult firstDerivative(RollupSeries rollupSeries,QueryRecords.Slice slice);

  QueryRecords.QueryResult aggregateSum(RollupSeries rollupSeries, QueryRecords.Slice slice);
}
