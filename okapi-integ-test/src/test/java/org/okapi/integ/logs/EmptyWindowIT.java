package org.okapi.integ.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.okapi.rest.logs.QueryRequest;

public class EmptyWindowIT extends LogsHttpSupport {

  @Test
  void emptyWindow_returnsEmptyItems() throws Exception {
    String tenant = newTenantId();
    String stream = "s-integ";

    // Query a far past window without ingest
    long now = Instant.now().toEpochMilli();
    long start = now - 24 * 3600_000L;
    long end = now - 24 * 3600_000L + 60_000;
    var resp = query(tenant, stream, new org.okapi.rest.logs.QueryRequest(start, end, 100, null, regexFilter(".*")));
    int size = (resp.items == null) ? 0 : resp.items.size();
    assertEquals(0, size);
  }
}

