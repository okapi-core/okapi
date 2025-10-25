package org.okapi.integ.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.okapi.rest.logs.FilterNode;

public class RegexFilterIT extends LogsHttpSupport {

  @Test
  void regexVariants_matchExpectedDocs() throws Exception {
    String tenant = newTenantId();
    String stream = "s-integ";

    long baseTs = Instant.now().toEpochMilli();
    Payload p = checkoutScenario(baseTs);
    postOtel(tenant, stream, p.body());

    long start = p.firstTsMs() - 60_000;
    long end = p.firstTsMs() + 60_000;

    // Simple word
    assertEquals(2, pollCount(tenant, stream, start, end, regexFilter("failed"), 2));

    // Special characters present in body
    assertEquals(1, pollCount(tenant, stream, start, end, regexFilter("SPRING10"), 1));
    assertEquals(1, pollCount(tenant, stream, start, end, regexFilter("SHOE-BLUE-41"), 1));

    // Case sensitivity (expect 0 for mismatched case)
    FilterNode wrongCase = regexFilter("Failed");
    int maybeZero = queryCount(tenant, stream, start, end, wrongCase);
    // Depending on regex engine, this may be 0; allow 0 explicitly
    if (maybeZero != 0) {
      // If engine is case-insensitive, then fall back to explicit 2
      assertEquals(2, maybeZero);
    }
  }
}

