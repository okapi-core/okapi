package org.okapi.metrics.rollup;

import java.util.Optional;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.stats.Statistics;

public interface TsReader {
  ScanResult scan(String series, long from, long to, AGG_TYPE aggregation, RES_TYPE resolution);

  int count(String series, long from, long to, RES_TYPE resolution);

  /**
   * `when` indicates a point in time denoted by linux epoch. The returned is the statistic within
   * the specific bucket. for secondly, this the statistic within the time bucket [nearest second to
   * `when`, 1s + nearest second to `when`] for minutely, this the statistic within the time bucket
   * [nearest minute to `when`, 1min + nearest min to `when`] for hourly, this the statistic within
   * the time bucket [nearest hour to `when`, 1hr + nearest min to `when`]
   * If no metrics were emitted within this window then the function returns an empty Optional.
   */
  Optional<Statistics> secondlyStats(String series, long when);

  Optional<Statistics> minutelyStats(String series, long when);

  Optional<Statistics> hourlyStats(String series, long when);
  Optional<Statistics> getStat(String key);
}
