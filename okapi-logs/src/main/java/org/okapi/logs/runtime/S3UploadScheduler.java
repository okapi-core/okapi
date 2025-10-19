package org.okapi.logs.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.logs.config.LogsConfigProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class S3UploadScheduler {
  private final LogsConfigProperties cfg;
  private final S3Client s3Client;
  private final NodeIdSupplier nodeIdSupplier;

  @Scheduled(fixedDelayString = "${okapi.logs.s3UploadIntervalMs:60000}")
  public void onTick() {
    if (!cfg.isS3UploadEnabled()) return;
    if (cfg.getS3Bucket() == null || cfg.getS3Bucket().isEmpty()) return;
    try {
      scanAndUpload();
    } catch (Exception e) {
      log.warn("S3 upload scan failed", e);
    }
  }

  private void scanAndUpload() throws IOException {
    Path base = Path.of(cfg.getDataDir());
    if (!Files.exists(base)) return;
    long nowMs = System.currentTimeMillis();
    long cutoffHour = floorToHour(nowMs - cfg.getS3UploadGraceMs());
    Files.walk(base, 4)
        .filter(Files::isDirectory)
        .filter(p -> p.getNameCount() >= base.getNameCount() + 3)
        .forEach(
            hourDir -> {
              String hourStr = hourDir.getFileName().toString();
              if (!isHourDirName(hourStr)) return;
              long hourStart = hourToEpochMs(hourStr);
              if (hourStart > cutoffHour) return; // not finalized yet
              try {
                Path streamDir = hourDir.getParent();
                Path tenantDir = streamDir.getParent();
                String tenant = tenantDir.getFileName().toString();
                String stream = streamDir.getFileName().toString();
                uploadHourDir(tenant, stream, hourStr, hourDir);
              } catch (Exception e) {
                log.warn("Failed to upload {}", hourDir, e);
              }
            });
  }

  private void uploadHourDir(String tenant, String stream, String hour, Path hourDir)
      throws IOException {
    Path bin = hourDir.resolve("logfile.bin");
    Path idx = hourDir.resolve("logfile.idx");
    if (!Files.exists(bin) || !Files.exists(idx)) return;

    // Idempotency: compare marker
    Path marker = hourDir.resolve(".upload.complete");
    Map<String, Object> current = Map.of(
        "binSize", Files.size(bin),
        "binMtime", Files.getLastModifiedTime(bin).toMillis(),
        "idxSize", Files.size(idx),
        "idxMtime", Files.getLastModifiedTime(idx).toMillis());
    if (Files.exists(marker)) {
      String content = Files.readString(marker);
      if (content.equals(current.toString())) return; // unchanged
    }

    String basePrefix = (cfg.getS3BasePrefix() == null ? "logs" : cfg.getS3BasePrefix());
    String nodeSeg = cfg.isS3UploadIncludeNodeId() ? "/" + nodeIdSupplier.getNodeId() : "";
    String prefix = basePrefix + "/" + tenant + "/" + stream + "/" + hour + nodeSeg;

    s3Client.putObject(
        PutObjectRequest.builder().bucket(cfg.getS3Bucket()).key(prefix + "/logfile.idx").build(),
        RequestBody.fromBytes(Files.readAllBytes(idx)));
    s3Client.putObject(
        PutObjectRequest.builder().bucket(cfg.getS3Bucket()).key(prefix + "/logfile.bin").build(),
        RequestBody.fromBytes(Files.readAllBytes(bin)));

    // Write marker
    Files.writeString(marker, current.toString());
  }

  private static boolean isHourDirName(String name) {
    return name.length() == 10 && name.chars().allMatch(Character::isDigit);
  }

  private static long hourToEpochMs(String hour) {
    int year = Integer.parseInt(hour.substring(0, 4));
    int mon = Integer.parseInt(hour.substring(4, 6));
    int day = Integer.parseInt(hour.substring(6, 8));
    int hr = Integer.parseInt(hour.substring(8, 10));
    return ZonedDateTime.of(year, mon, day, hr, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();
  }

  private static long floorToHour(long tsMs) {
    ZonedDateTime z = Instant.ofEpochMilli(tsMs).atZone(ZoneId.of("UTC"));
    return z.withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli();
  }
}

