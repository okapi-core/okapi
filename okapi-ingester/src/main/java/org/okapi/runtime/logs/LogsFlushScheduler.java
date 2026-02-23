package org.okapi.runtime.logs;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.okapi.logs.LogsBufferPool;
import org.okapi.logs.config.LogsCfg;
import org.okapi.spring.configs.Profiles;
import org.okapi.spring.configs.properties.LogsCfgImpl;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@Component
@EnableScheduling
@Slf4j
public class LogsFlushScheduler {
  private final LogsBufferPool pool;
  private final LogsCfg logsCfg;

  public LogsFlushScheduler(LogsCfgImpl cfg, LogsBufferPool pool) {
    this.pool = pool;
    this.logsCfg = cfg;
  }

  public void onTick() {
    Instant now = Instant.now();
    pool.flushPagesOlderThan(
        now.minus(logsCfg.getIdxExpiryDuration(), ChronoUnit.MILLIS).toEpochMilli());
  }
}
