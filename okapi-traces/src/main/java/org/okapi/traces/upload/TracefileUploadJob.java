package org.okapi.traces.upload;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.traces.NodeIdSupplier;
import org.okapi.traces.query.S3TracefileKeyResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Component
@RequiredArgsConstructor
public class TracefileUploadJob {

  private final S3Client s3Client;
  private final S3TracefileKeyResolver keyResolver;
  private final NodeIdSupplier nodeIdSupplier;

  @Value("${okapi.traces.baseDir:traces}")
  private Path baseDir;

  private final ReentrantLock lock = new ReentrantLock();

  @Scheduled(fixedDelayString = "${okapi.traces.uploadIntervalMs:600000}") // default 10 minutes
  public void runUpload() {
    if (!lock.tryLock()) return;
    try {
      long nowBucket = System.currentTimeMillis() / 3_600_000L;
      if (!Files.exists(baseDir)) return;

      Files.walk(baseDir)
          .filter(
              p ->
                  p.getFileName().toString().startsWith("tracefile.")
                      && p.getFileName().toString().endsWith(".bin"))
          .forEach(
              p -> {
                try {
                  String name = p.getFileName().toString();
                  String hbStr =
                      name.substring("tracefile.".length(), name.length() - ".bin".length());
                  long hb = Long.parseLong(hbStr);
                  if (hb >= nowBucket) return; // not older than 1h

                  // derive tenant/app from path: baseDir/tenant/app/tracefile.hb.bin
                  Path rel = baseDir.relativize(p.getParent());
                  if (rel.getNameCount() < 2) return;
                  String tenant = rel.getName(rel.getNameCount() - 2).toString();
                  String app = rel.getName(rel.getNameCount() - 1).toString();

                  String bucket = keyResolver.bucket();
                  String key = keyResolver.uploadKey(tenant, app, hb, nodeIdSupplier.getNodeId());
                  log.info("Uploading {} to s3://{}/{}", p, bucket, key);
                  new TracefileMultipartUploader(s3Client).upload(p, bucket, key);
                  Files.deleteIfExists(p);
                } catch (Exception e) {
                  log.warn("Failed to upload tracefile {}", p, e);
                }
              });
    } catch (Exception e) {
      log.warn("Tracefile upload job failed", e);
    } finally {
      lock.unlock();
    }
  }
}
