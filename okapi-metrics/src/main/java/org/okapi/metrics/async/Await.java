package org.okapi.metrics.async;

import java.time.Duration;
import java.util.function.Supplier;

public class Await {

  public static boolean waitFor(Supplier<Boolean> cond, Duration poll, Duration maxWait)
      throws InterruptedException {
    var now = System.currentTimeMillis();
    var stopTime = now + maxWait.toMillis();
    var reached = false;
    while (now < stopTime) {
      now = System.currentTimeMillis();
      reached = cond.get();
      if (reached) break;
      Thread.sleep(poll.toMillis());
    }
    return reached;
  }
}
