package org.okapi.metrics.cas;

import java.util.concurrent.Semaphore;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ThrottledFn implements Runnable {

  Semaphore semaphore;
  Runnable task;

  @Override
  public void run() {
    try {
      semaphore.acquire();
      task.run();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      semaphore.release();
    }
  }
}
