package org.okapi.runtime.metrics;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.pages.MetricsBufferPool;
import org.okapi.spring.configs.Profiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@Component
public class MetricsFlushScheduler {

  private final MetricsBufferPool pool;
  private final MetricsCfg metricsCfg;

  public MetricsFlushScheduler(MetricsBufferPool pool, MetricsCfg metricsCfg) {
    this.pool = pool;
    this.metricsCfg = metricsCfg;
  }

  // no @Scheduled here anymore
  public void onTick() {
    Instant now = Instant.now();
    long cutoffEpochMs =
        now.minus(metricsCfg.getIdxExpiryDuration(), ChronoUnit.MILLIS).toEpochMilli();

    pool.flushPagesOlderThan(cutoffEpochMs);
  }
}
