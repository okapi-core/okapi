package org.okapi.logs.runtime;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.okapi.logs.config.LogsCfgImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
public class HourlyFlushScheduler {
  private final LogPageBufferPool pool;
  private final AtomicLong lastHourFlushed;

  public HourlyFlushScheduler(LogsCfgImpl cfg, LogPageBufferPool pool) {
    this.pool = pool;
    this.lastHourFlushed = new AtomicLong(-1);
  }

  @Scheduled(fixedDelay = 30000)
  public void onTick() {
    Instant now = Instant.now();
    long hourKey = hourKey(now.toEpochMilli());

    // If hour changed since last run, ensure previous hour is finalized
    long last = lastHourFlushed.get();
    if (last != hourKey) {
      // Flush pages overlapping previous hour (tsStart < current hour start)
      pool.flushPagesOlderThan(now.minus(1, ChronoUnit.HOURS).toEpochMilli());
      // S3 uploads are handled by S3UploadScheduler
      lastHourFlushed.set(hourKey);
    }
  }

  private long hourKey(long millis) {
    return millis / 1000 / 3600;
  }
}
