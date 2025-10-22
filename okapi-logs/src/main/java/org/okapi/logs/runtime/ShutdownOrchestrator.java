package org.okapi.logs.runtime;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.swim.membership.PodLifecycle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShutdownOrchestrator {
  private final LogPageBufferPool pool;
  private final S3UploadService uploader;
  private final PodLifecycle podLifecycle;

  @PreDestroy
  public void onShutdown() {
    try {
      // 1) Flush all pages to disk
      pool.flushAllNow();
      pool.awaitFlushQueueEmpty(5000);

      // 2) Upload current hour
      long now = System.currentTimeMillis();
      long hour = (now / 3600_000L);
      uploader.uploadHour(hour);
    } catch (Exception e) {
      log.warn("Error during shutdown flush/upload", e);
    } finally {
      // 3) Emit pod_delete even if upload failed
      try {
        podLifecycle.emitPodDelete();
      } catch (Exception e) {
        log.warn("Failed to emit pod_delete", e);
      }
    }
  }
}

