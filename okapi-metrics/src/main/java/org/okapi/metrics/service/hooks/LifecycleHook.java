package org.okapi.metrics.service.hooks;

import java.util.concurrent.ScheduledExecutorService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.rollup.WriteBackSettings;
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

  @Autowired Node node;
  @Autowired MetricsHandlerImpl metricsHandler;

  @Autowired ScheduledExecutorService scheduledExecutorService;
  @Autowired RocksStore rocksStore;
  @Autowired WriteBackSettings writeBackSettings;

  void startHandler() throws Exception {
    metricsHandler.onStart();
  }

  @SneakyThrows
  @Override
  public void start() {
    startHandler();
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
