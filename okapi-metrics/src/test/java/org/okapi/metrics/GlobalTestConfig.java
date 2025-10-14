package org.okapi.metrics;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.awaitility.core.ConditionFactory;

public class GlobalTestConfig {

  public static final Duration WAIT_TIME = Duration.of(10, ChronoUnit.SECONDS);

  public static ConditionFactory okapiWait() {
    return await().atMost(WAIT_TIME);
  }
}
