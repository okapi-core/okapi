package org.okapi.metrics.service.runnables;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
public class BackgroundJobs {

  ScheduledExecutorService scheduledExecutorService;
  ClusterChangeListener clusterChangeListener;

  public void trigger() {
    log.info("Triggering background tasks.");
    scheduledExecutorService.schedule(clusterChangeListener, 0, TimeUnit.SECONDS);
  }
}
