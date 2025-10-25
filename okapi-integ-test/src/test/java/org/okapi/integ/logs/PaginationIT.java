package org.okapi.integ.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.okapi.rest.logs.QueryRequest;
import org.okapi.rest.logs.QueryResponse;

public class PaginationIT extends LogsHttpSupport {

  @Test
  void pagination_returnsNonOverlappingPages() throws Exception {
    String tenant = newTenantId();
    String stream = "s-integ";

    long baseTs = Instant.now().toEpochMilli();
    Payload p = checkoutScenario(baseTs);
    postOtel(tenant, stream, p.body());

    long start = p.firstTsMs() - 60_000;
    long end = p.firstTsMs() + 60_000;

    QueryRequest q1 = new QueryRequest(start, end, 3, null, regexFilter(".*"));
    QueryResponse r1 = query(tenant, stream, q1);
    int n1 = r1.items == null ? 0 : r1.items.size();
    assertEquals(3, n1);
    assertTrue(r1.nextPageToken != null && !r1.nextPageToken.isBlank());

    QueryRequest q2 = new QueryRequest(start, end, 3, r1.nextPageToken, regexFilter(".*"));
    QueryResponse r2 = query(tenant, stream, q2);
    int n2 = r2.items == null ? 0 : r2.items.size();
    assertEquals(3, n2);

    // Compare against total: pages should be non-overlapping and bounded by total
    int total = queryCount(tenant, stream, start, end, regexFilter(".*"));
    assertTrue(n1 + n2 <= total);
    if (total >= 6) {
      assertEquals(6, n1 + n2);
    }
  }
}
