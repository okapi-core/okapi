package org.okapi.metrics.service;

import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.WriteBackRequest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ServiceControllerImpl implements ServiceController {
  AtomicBoolean canConsume;
  AtomicBoolean processRunning;
  CountDownLatch consumerStopped;
  String id;
  SharedMessageBox<WriteBackRequest> messageBox;

  public ServiceControllerImpl(String id, SharedMessageBox<WriteBackRequest> messageBox) {
    canConsume = new AtomicBoolean(true);
    processRunning = new AtomicBoolean(true);
    consumerStopped = new CountDownLatch(0);
    this.id = id;
    this.messageBox = messageBox;
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

  @Override
  public boolean isBoxEmpty() {
    return this.messageBox.isEmpty();
  }
}
