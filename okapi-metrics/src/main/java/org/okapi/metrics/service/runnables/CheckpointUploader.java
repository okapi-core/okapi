package org.okapi.metrics.service.runnables;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.rollup.FrozenMetricsUploader;
import org.okapi.metrics.service.ExponentialBackoffCalculator;
import org.okapi.metrics.service.ServiceController;

@Slf4j
public class CheckpointUploader implements Runnable {
  FrozenMetricsUploader frozenMetricsUploader;
  ScheduledExecutorService scheduledExecutorService;
  Integer failedCount = 0;
  ServiceController serviceController;
  Duration checkpointDelay;

  public CheckpointUploader(
      FrozenMetricsUploader frozenMetricsUploader,
      ScheduledExecutorService scheduledExecutorService,
      ServiceController serviceController,
      Duration checkpointDelay) {
    this.frozenMetricsUploader = frozenMetricsUploader;
    this.scheduledExecutorService = scheduledExecutorService;
    this.serviceController = serviceController;
    this.checkpointDelay = checkpointDelay;
  }

  private void reschedule() {
    scheduledExecutorService.schedule(this, checkpointDelay.getSeconds(), TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    try {
      log.info("Uploading hourly checkpoint.");
      if (!serviceController.canConsume()) {
        log.info("Skipping this run as we're possibly resharding.");
        reschedule();
        return;
      }
      frozenMetricsUploader.uploadHourlyCheckpoint();
      failedCount = 0;
      reschedule();
    } catch (Exception e) {
      log.error("Could not upload checkpoint due to ", e);
      log.error(e.getMessage());
      failedCount++;
      var waitTime = waitTime(failedCount);
      if (failedCount == 5) {
        reschedule();
      } else {
        scheduledExecutorService.schedule(this, waitTime.getSeconds(), TimeUnit.SECONDS);
      }
    }
  }

  public Duration waitTime(int trial) {
    return ExponentialBackoffCalculator.wait(Duration.of(1, ChronoUnit.MINUTES), 2, trial);
  }
}
