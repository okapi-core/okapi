package org.okapi.metrics.service.runnables;

import static org.okapi.exceptions.ExceptionUtils.debugFriendlyMsg;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.service.ExponentialBackoffCalculator;
import org.okapi.metrics.service.ServiceController;
import org.okapi.metrics.sharding.LeaderJobs;

@Slf4j
@AllArgsConstructor
public class LeaderResponsibilityRunnable implements Runnable {
  ScheduledExecutorService scheduledExecutorService;
  LeaderJobs leaderJobs;
  ServiceController controller;

  @Override
  public void run() {
    try {
      leaderJobs.checkFleetHealth();
      leaderJobs.checkShardMovementStatus();
    } catch (Exception e) {
      log.error("Failed due to {}", debugFriendlyMsg(e));
      throw new RuntimeException(e);
    } finally {
      if (controller.isProcessRunning()) {
        var waitTime = ExponentialBackoffCalculator.jitteryWait(5_000, 10_000);
        scheduledExecutorService.schedule(this, waitTime, TimeUnit.MILLISECONDS);
      } else {
        log.info("Stopping leadership checker.");
      }
    }
  }
}
