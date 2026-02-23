package org.okapi.wal.consumer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Locked;

public class WalConsumerController {
  boolean canConsume = false;
  CountDownLatch ackStop = new CountDownLatch(0);
  CountDownLatch ackStart = new CountDownLatch(0);
  Lock startStopLock = new ReentrantLock();

  @Locked("startStopLock")
  public void stop() {
    canConsume = false;
    ackStop = new CountDownLatch(1);
  }

  @Locked("startStopLock")
  public void start() {
    canConsume = true;
    ackStart = new CountDownLatch(1);
  }

  @Locked("startStopLock")
  public void stopAcked() {
    ackStop.countDown();
  }

  @Locked("startStopLock")
  public void startAcked() {
    ackStart.countDown();
  }

  @Locked("startStopLock")
  public boolean canConsume() {
    return canConsume;
  }

  public boolean stopIsAcked(long timeMillis) throws InterruptedException {
    return ackStop.await(timeMillis, TimeUnit.MILLISECONDS);
  }

  public boolean startIsAcked(long timeMillis) throws InterruptedException {
    return ackStart.await(timeMillis, TimeUnit.MILLISECONDS);
  }
}
