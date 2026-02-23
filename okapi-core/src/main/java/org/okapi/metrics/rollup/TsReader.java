/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.rollup;

import java.util.Map;
import java.util.Optional;
import org.okapi.Statistics;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.pojos.SUM_TYPE;
import org.okapi.metrics.pojos.results.GaugeScan;
import org.okapi.metrics.pojos.results.HistoScan;
import org.okapi.metrics.pojos.results.SumScan;

public interface TsReader {
  GaugeScan scanGauge(String series, long from, long to, AGG_TYPE aggregation, RES_TYPE resolution);

  HistoScan scanHisto(String series, long from, long to);

  SumScan scanSum(String series, long from, long to, long windowSize, SUM_TYPE sumType);

  Map<Long, ? extends Statistics> scanGauge(String series, long from, long to, RES_TYPE resolution);

  /**
   * `when` indicates a point in time denoted by linux epoch. The returned is the statistic within
   * the specific bucket. for secondly, this the statistic within the time bucket [nearest second to
   * `when`, 1s + nearest second to `when`] for minutely, this the statistic within the time bucket
   * [nearest minute to `when`, 1min + nearest min to `when`] for hourly, this the statistic within
   * the time bucket [nearest hour to `when`, 1hr + nearest min to `when`] If no metrics were
   * emitted within this window then the function returns an empty Optional.
   */
  Optional<Statistics> secondlyStats(String series, long when);

  Optional<Statistics> minutelyStats(String series, long when);

  Optional<Statistics> hourlyStats(String series, long when);

  Optional<Statistics> getStat(String key);
}
