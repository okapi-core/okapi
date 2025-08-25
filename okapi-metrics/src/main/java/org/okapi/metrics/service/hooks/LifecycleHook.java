package org.okapi.metrics.service.hooks;

import java.util.concurrent.ScheduledExecutorService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.RocksDbStatsWriter;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.coordinator.CentralCoordinator;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.service.*;
import org.okapi.metrics.service.runnables.BackgroundJobs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LifecycleHook implements SmartLifecycle {
  boolean isRunning;

  @Autowired BackgroundJobs backgroundJobs;

  @Autowired(required = false)
  CentralCoordinator centralCoordinator;

  @Autowired Node node;
  @Autowired MetricsHandlerImpl metricsHandler;

  @Autowired ScheduledExecutorService scheduledExecutorService;

  @Autowired RocksDbStatsWriter rocksDbStatsWriter;
  @Autowired RocksStore rocksStore;

  void startHandler() throws Exception {
    metricsHandler.onStart();
  }

  private void doCoordinatorStuff() throws Exception {
    if (centralCoordinator == null) return;
    centralCoordinator.registerWatchersForIdLoss(node);
  }

  @SneakyThrows
  @Override
  public void start() {
    doCoordinatorStuff();
    startHandler();
    rocksDbStatsWriter.startWriting(scheduledExecutorService, rocksStore);
    backgroundJobs.trigger();
    isRunning = true;
  }

  @SneakyThrows
  @Override
  public void stop() {
    isRunning = false;
  }

  @Override
  public boolean isRunning() {
    return isRunning;
  }
}
