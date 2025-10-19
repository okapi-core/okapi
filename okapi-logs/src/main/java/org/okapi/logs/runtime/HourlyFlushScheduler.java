package org.okapi.logs.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.okapi.logs.config.LogsConfigProperties;
import org.okapi.logs.io.LogFileWriter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
public class HourlyFlushScheduler {
  private final LogsConfigProperties cfg;
  private final LogPageBufferPool pool;
  private final LogFileWriter writer;
  private final AtomicLong lastHourFlushed;

  public HourlyFlushScheduler(LogsConfigProperties cfg, LogPageBufferPool pool) {
    this.cfg = cfg;
    this.pool = pool;
    this.writer = new LogFileWriter(cfg);
    this.lastHourFlushed = new AtomicLong(-1);
  }

  @Scheduled(fixedDelay = 30000)
  public void onTick() {
    long nowMs = System.currentTimeMillis();
    ZonedDateTime now = Instant.ofEpochMilli(nowMs).atZone(ZoneId.of("UTC"));
    long hourKey = hourKey(now);
    long prevHourKey = hourKey - 1;
    // If hour changed since last run, ensure previous hour is finalized
    long last = lastHourFlushed.get();
    if (last != hourKey) {
      // Flush pages overlapping previous hour (tsStart < current hour start)
      pool.flushPagesOlderThan(
          now.withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli());
      // S3 uploads are handled by S3UploadScheduler
      lastHourFlushed.set(hourKey);
    }
  }

  private long hourKey(ZonedDateTime zdt) {
    return zdt.getYear() * 1000000L
        + zdt.getMonthValue() * 10000L
        + zdt.getDayOfMonth() * 100L
        + zdt.getHour();
  }

  // S3 upload logic removed; handled by S3UploadScheduler
}
