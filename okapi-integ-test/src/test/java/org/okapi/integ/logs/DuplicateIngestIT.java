package org.okapi.integ.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

public class DuplicateIngestIT extends LogsHttpSupport {

  @Test
  void duplicateIngest_doublesCounts() throws Exception {
    String tenant = newTenantId();
    String stream = "s-integ";

    long baseTs = Instant.now().toEpochMilli();
    Payload p = checkoutScenario(baseTs);
    postOtel(tenant, stream, p.body());
    postOtel(tenant, stream, p.body());

    long start = p.firstTsMs() - 60_000;
    long end = p.firstTsMs() + 60_000;

    // TRACE A has 5 docs; duplicated ingest → 10
    assertEquals(10, pollCount(tenant, stream, start, end, traceFilter(TRACE_A_HEX), 10));
    // "failed" appears twice per corpus; duplicated → 4
    assertEquals(4, pollCount(tenant, stream, start, end, regexFilter("failed"), 4));
  }
}

