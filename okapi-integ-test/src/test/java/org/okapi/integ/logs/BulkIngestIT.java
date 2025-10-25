package org.okapi.integ.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.rest.logs.FilterNode;

public class BulkIngestIT extends LogsHttpSupport {

  @Test
  void bulkIngest_writesAndQueries() throws Exception {
    String tenant = newTenantId();
    String stream = "s-integ";

    long baseTs = Instant.now().toEpochMilli();
    byte[] scope = scopeLogsBytes(baseTs, List.of("a","b","c","d","e"), List.of(9,9,13,11,17));
    postBulk(tenant, stream, scope);

    long start = baseTs - 60_000;
    long end = baseTs + 60_000;

    // Any regex that matches all but ensure set size 5
    FilterNode any = regexFilter(".");
    assertEquals(5, pollCount(tenant, stream, start, end, any, 5));
  }
}

