/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import java.time.Duration;
import org.okapi.metrics.rollup.TsReader;

public interface QueryProcessor {
  QueryRecords.QueryResult scan(TsReader reader, QueryRecords.Slice slice);

  QueryRecords.QueryResult scale(TsReader reader, QueryRecords.Slice slice, float scaleFactor);

  QueryRecords.QueryResult sum(TsReader reader, QueryRecords.Slice left, QueryRecords.Slice right);

  float count(TsReader reader, QueryRecords.Slice slice);

  QueryRecords.QueryResult transform(
      TsReader reader, QueryRecords.Slice slice, TRANSFORM transform);

  QueryRecords.QueryResult movingAverage(
      TsReader reader, QueryRecords.Slice slice, Duration windowSize);

  QueryRecords.QueryResult movingSum(
      TsReader reader, QueryRecords.Slice slice, Duration windowSize);

  QueryRecords.QueryResult firstDerivative(TsReader reader, QueryRecords.Slice slice);

  QueryRecords.QueryResult aggregateSum(TsReader reader, QueryRecords.Slice slice);

  public enum TRANSFORM {
    LOG,
    SIGMOID
  }
}
