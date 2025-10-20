package org.okapi.logs.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class LogsHealthIndicator implements HealthIndicator {

  @Override
  public Health health() {
    // For now, just report UP. Extend to include queue length, etc.
    return Health.up().build();
  }
}

