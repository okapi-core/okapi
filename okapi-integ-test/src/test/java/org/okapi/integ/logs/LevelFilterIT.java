package org.okapi.integ.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

public class LevelFilterIT extends LogsHttpSupport {

  @Test
  void levelWarnAndError_countsMatch() throws Exception {
    String tenant = newTenantId();
    String stream = "s-integ";

    long baseTs = Instant.now().toEpochMilli();
    Payload p = checkoutScenario(baseTs);
    postOtel(tenant, stream, p.body());

    long start = p.firstTsMs() - 60_000;
    long end = p.firstTsMs() + 60_000;

    assertEquals(2, pollCount(tenant, stream, start, end, levelFilter(30), 2)); // WARN
    assertEquals(2, pollCount(tenant, stream, start, end, levelFilter(40), 2)); // ERROR
  }
}

