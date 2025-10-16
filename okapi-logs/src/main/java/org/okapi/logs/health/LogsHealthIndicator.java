package org.okapi.logs.health;

import java.util.concurrent.atomic.AtomicBoolean;
import org.okapi.logs.runtime.LogPageBufferPool;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class LogsHealthIndicator implements HealthIndicator {
  private final LogPageBufferPool pool;

  public LogsHealthIndicator(LogPageBufferPool pool) {
    this.pool = pool;
  }

  @Override
  public Health health() {
    // For now, just report UP. Extend to include queue length, etc.
    return Health.up().build();
  }
}

