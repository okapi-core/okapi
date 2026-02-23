package org.okapi.retries;

import java.util.concurrent.Callable;

public class RetryingCallables {

  public static void retry(Callable<Boolean> callable, int maxRetries, long delayMillis) {
    int attempts = 0;
    while (attempts < maxRetries) {
      boolean isDone = false;
      try {
        isDone = callable.call();
      } catch (Exception e) {
        attempts++;
      }
      if (isDone) return; // Success
      else attempts++;
    }
    try {
      Thread.sleep(delayMillis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
