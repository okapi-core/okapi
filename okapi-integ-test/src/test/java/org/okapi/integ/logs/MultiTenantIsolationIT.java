package org.okapi.integ.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

public class MultiTenantIsolationIT extends LogsHttpSupport {

  @Test
  void tenants_areIsolated() throws Exception {
    String tenantA = newTenantId();
    String tenantB = newTenantId();
    String stream = "s-integ";

    long baseTs = Instant.now().toEpochMilli();
    Payload p = checkoutScenario(baseTs);
    postOtel(tenantA, stream, p.body());
    postOtel(tenantB, stream, p.body());

    long start = p.firstTsMs() - 60_000;
    long end = p.firstTsMs() + 60_000;

    // Each tenant should see its own 5 traceA docs
    assertEquals(5, pollCount(tenantA, stream, start, end, traceFilter(TRACE_A_HEX), 5));
    assertEquals(5, pollCount(tenantB, stream, start, end, traceFilter(TRACE_A_HEX), 5));
  }
}

