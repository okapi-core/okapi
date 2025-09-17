package org.okapi.metrics.query.promql;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.common.pojo.Node;

public class PromQLSmallTestCase extends PromQlTestCase {

  public static final String TENANT_ID = "PromQlSmallTenant";
  public static final String PATH = TENANT_ID + ":http_requests{}";

  long t0, t1, t2, t3;

  public PromQLSmallTestCase(
      long now,
      TestResourceFactory resourceFactory,
      Node node) {
    super();
    t0 = now - Duration.of(5, ChronoUnit.MINUTES).toMillis(); // arbitrary fixed base
    long step = 60_000L; // 1 minute
    t1 = t0 + step; // now - 4
    t2 = t1 + step; // now - 3
    t3 = t2 + step; // now - 2
    setBatch(0, PATH, new long[] {t0, t1, t2, t3}, new float[] {10f, 20f, 30f, 40f});
  }
}
