package org.okapi.logs.runtime;

import static org.mockito.Mockito.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.runtime.S3UploadScheduler;
import org.okapi.runtime.logs.LogsFilesS3Uploader;
import org.okapi.runtime.metrics.MetricFilesS3Uploader;
import org.okapi.runtime.spans.TraceFilesS3Uploader;

class S3UploadSchedulerTest {

  @TempDir Path tempDir;

  @Test
  void uploads_oldHour_andSkips_currentHour_andRespectsMarker() throws Exception {
    var traceUploadService = mock(TraceFilesS3Uploader.class);
    var logUploadService = mock(LogsFilesS3Uploader.class);
    var metricsUploadService = mock(MetricFilesS3Uploader.class);
    var uploadScheduler =
        new S3UploadScheduler(traceUploadService, logUploadService, metricsUploadService);
    uploadScheduler.onTick();
    verify(traceUploadService, times(1)).uploadBlockNoThrow();
    verify(logUploadService, times(1)).uploadBlockNoThrow();
    verify(metricsUploadService, times(1)).uploadBlockNoThrow();
  }

  @Test
  void doesNotThrowOnException() throws Exception {
    var traceUploadService = mock(TraceFilesS3Uploader.class);
    var logUploadService = mock(LogsFilesS3Uploader.class);
    var metricsUploadService = mock(MetricFilesS3Uploader.class);
    var uploadScheduler =
        new S3UploadScheduler(traceUploadService, logUploadService, metricsUploadService);
    doThrow(new RuntimeException("Test exception")).when(traceUploadService).uploadBlock();
    uploadScheduler.onTick();
    verify(metricsUploadService, times(1)).uploadBlockNoThrow();
    verify(logUploadService, times(1)).uploadBlockNoThrow();
    verify(traceUploadService, times(1)).uploadBlockNoThrow();
  }
}
