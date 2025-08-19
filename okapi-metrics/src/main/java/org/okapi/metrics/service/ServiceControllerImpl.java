package org.okapi.metrics.service;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ServiceControllerImpl implements ServiceController {
  AtomicBoolean canConsume;
  AtomicBoolean processRunning;
  CountDownLatch consumerStopped;
  String id;

  public ServiceControllerImpl(String id) {
    canConsume = new AtomicBoolean(true);
    processRunning = new AtomicBoolean(true);
    consumerStopped = new CountDownLatch(0);
    this.id = id;
  }

  @Override
  public boolean canConsume() {
    return canConsume.get() && processRunning.get();
  }

  @Override
  public void pauseConsumer() {
    log.info("Pausing consumer on " + id);
    consumerStopped = new CountDownLatch(1);
    canConsume.set(false);
  }

  @Override
  public boolean resumeConsumer() {
    canConsume.set(true);
    return canConsume.get();
  }

  @Override
  public boolean startProcess() {
    processRunning.set(true);
    return processRunning.get();
  }

  @Override
  public void stopProcess() {
    processRunning.set(false);
  }

  @Override
  public boolean isProcessRunning() {
    return processRunning.get();
  }
}
