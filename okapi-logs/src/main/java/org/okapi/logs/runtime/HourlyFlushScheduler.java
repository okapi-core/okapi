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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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
      // Upload previous hour partition to S3
      if (cfg.getS3Bucket() != null && !cfg.getS3Bucket().isEmpty()) {
        uploadHour(prevHourKey);
      }
      lastHourFlushed.set(hourKey);
    }
  }

  private long hourKey(ZonedDateTime zdt) {
    return zdt.getYear() * 1000000L
        + zdt.getMonthValue() * 10000L
        + zdt.getDayOfMonth() * 100L
        + zdt.getHour();
  }

  private void uploadHour(long hourKey) {
    // Walk local dirs for all tenant/streams and upload files for this hour
    Path base = Path.of(cfg.getDataDir());
    if (!Files.exists(base)) return;
    try {
      String hourStr = String.format("%010d", hourKey);
      Files.walk(base, 4)
          .filter(Files::isDirectory)
          .filter(p -> p.getNameCount() >= base.getNameCount() + 3)
          .filter(p -> p.getFileName().toString().equals(hourStr))
          .forEach(
              hourDir -> {
                try {
                  Path streamDir = hourDir.getParent();
                  Path tenantDir = streamDir.getParent();
                  String tenant = tenantDir.getFileName().toString();
                  String stream = streamDir.getFileName().toString();
                  String hour = hourDir.getFileName().toString();
                  String prefix =
                      (cfg.getS3BasePrefix() == null ? "logs" : cfg.getS3BasePrefix())
                          + "/"
                          + tenant
                          + "/"
                          + stream
                          + "/"
                          + hour;
                  putIfExists(hourDir.resolve("logfile.bin"), prefix + "/logfile.bin");
                  putIfExists(hourDir.resolve("logfile.idx"), prefix + "/logfile.idx");
                } catch (Exception e) {
                  log.warn("Failed uploading hour dir {}", hourDir, e);
                }
              });
    } catch (IOException e) {
      log.warn("Error scanning hour directories", e);
    }
  }

  private void putIfExists(Path path, String key) throws IOException {
    if (!Files.exists(path)) return;
    S3Client s3 =
        S3Client.builder().region(Region.of(System.getProperty("aws.region", "us-east-1"))).build();
    s3.putObject(
        PutObjectRequest.builder().bucket(cfg.getS3Bucket()).key(key).build(),
        RequestBody.fromBytes(Files.readAllBytes(path)));
  }
}
