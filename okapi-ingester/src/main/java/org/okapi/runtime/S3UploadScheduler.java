/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.runtime;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.runtime.logs.LogsFilesS3Uploader;
import org.okapi.runtime.metrics.MetricFilesS3Uploader;
import org.okapi.runtime.spans.TraceFilesS3Uploader;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@AllArgsConstructor
public class S3UploadScheduler {
  TraceFilesS3Uploader traceFilesS3Uploader;
  LogsFilesS3Uploader logsFilesS3Uploader;
  MetricFilesS3Uploader metricFilesS3Uploader;

  public void onTick() {
    logsFilesS3Uploader.uploadBlockNoThrow();
    traceFilesS3Uploader.uploadBlockNoThrow();
    metricFilesS3Uploader.uploadBlockNoThrow();
  }
}
