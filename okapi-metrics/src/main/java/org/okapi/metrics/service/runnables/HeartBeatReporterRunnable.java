package org.okapi.metrics.service.runnables;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.service.ServiceController;

@Slf4j
@AllArgsConstructor
public class HeartBeatReporterRunnable implements Runnable {
  ServiceRegistry serviceRegistry;
  ScheduledExecutorService scheduledExecutorService;
  ServiceController controller;

  @Override
  public void run() {
    try {
      serviceRegistry.writeHeartBeat();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (controller.isProcessRunning()) {
        var waitTime = 5_000;
        log.debug("Next heart beat after {}s", waitTime);
        scheduledExecutorService.schedule(this, waitTime, TimeUnit.MILLISECONDS);
      } else {
        // If the process is stopped, we do not reschedule the heart beat reporter.
        log.info("Stopping heart beat reporter.");
      }
    }
  }
}
