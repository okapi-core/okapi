package org.okapi.runtime;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.okapi.logs.LogsBufferPool;
import org.okapi.pages.MetricsBufferPool;
import org.okapi.runtime.logs.LogsFilesS3Uploader;
import org.okapi.runtime.metrics.MetricFilesS3Uploader;
import org.okapi.runtime.spans.TraceFilesS3Uploader;
import org.okapi.traces.TracesBufferPool;

@Slf4j
public class ShutdownOrchestrator {
  /***
   * This file collects all buffer pools and uploaders and ensures that on shutdown all data is flushed out.
   */
  private final LogsBufferPool logsBufferPool;

  private final TracesBufferPool tracesBufferPool;
  private final MetricsBufferPool metricsBufferPool;
  private final TraceFilesS3Uploader traceUploader;
  private final LogsFilesS3Uploader logsUploader;
  private final MetricFilesS3Uploader metricFilesS3Uploader;

  public ShutdownOrchestrator(
      LogsBufferPool logsBufferPool,
      TracesBufferPool tracesBufferPool,
      MetricsBufferPool metricsBufferPool,
      TraceFilesS3Uploader traceUploader,
      LogsFilesS3Uploader logsUploader,
      MetricFilesS3Uploader metricFilesS3Uploader) {
    this.logsBufferPool = logsBufferPool;
    this.tracesBufferPool = tracesBufferPool;
    this.traceUploader = traceUploader;
    this.logsUploader = logsUploader;
    this.metricsBufferPool = metricsBufferPool;
    this.metricFilesS3Uploader = metricFilesS3Uploader;
  }

  @PreDestroy
  public void onShutdown() {
    try {
      // 1) Flush all pages to disk
      logsBufferPool.flushAllNow();
      tracesBufferPool.flushAllNow();
      metricsBufferPool.flushAllNow();
      logsBufferPool.awaitFlushQueueEmpty(5000);
      tracesBufferPool.awaitFlushQueueEmpty(5000);
      metricsBufferPool.awaitFlushQueueEmpty(5000);
      logsUploader.uploadAll();
      traceUploader.uploadAll();
      metricFilesS3Uploader.uploadAll();
    } catch (Exception e) {
      log.warn("Error during shutdown flush/upload", e);
    } finally {
      // 3) Emit pod_delete even if upload failed
    }
  }
}
