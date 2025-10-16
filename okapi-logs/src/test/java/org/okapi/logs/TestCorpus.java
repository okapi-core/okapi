package org.okapi.logs;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.okapi.logs.io.LogPage;

final class TestCorpus {
  private TestCorpus() {}

  static LogPage buildTestPage() {
    LogPage page =
        LogPage.builder()
            .traceIdSet(BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 100))
            .expectedInsertions(100)
            .build();
    long base = Instant.now().toEpochMilli();
    // Five for trace A (successful checkout)
    String tA = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // hex expected after mapping
    page.append(base + 1, tA, 20, "GET /api/catalog/search?q=running+shoes user=u123");
    page.append(base + 2, tA, 10, "PricingEngine applied discount code=SPRING10 order=1001");
    page.append(base + 3, tA, 30, "Inventory low for sku=SHOE-RED-42 remaining=2");
    page.append(base + 4, tA, 20, "Payment authorized order=1001 amount=79.99 provider=stripe");
    page.append(base + 5, tA, 20, "Order confirmed order=1001 user=u123");
    // Five for trace B (failed checkout)
    String tB = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    page.append(base + 6, tB, 20, "GET /api/catalog/product/SHOE-BLUE-41 user=u999");
    page.append(base + 7, tB, 40, "Payment authorization failed order=2002 amount=129.00 provider=stripe code=card_declined");
    page.append(base + 8, tB, 30, "Cart abandoned user=u999 after=2m");
    page.append(base + 9, tB, 40, "Order creation failed order=2002 cause=payment_error");
    page.append(base + 10, tB, 20, "Retry scheduled for payment order=2002 in=5m");
    return page;
  }
}
