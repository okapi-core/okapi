package org.okapi.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.logs.io.LogIngestRecord;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.query.*;
import org.okapi.queryproc.LogsQueryProcessor;

@AllArgsConstructor
public final class TestCorpus {
  @Getter
  long testStart;

  /**
   * Returns the full 10-record corpus as individual LogIngestRecord items, based off testStart.
   * Useful for integration tests that push records via Kafka.
   */
  public List<LogIngestRecord> getIndividualRecords() {
    long base = this.testStart;
    List<LogIngestRecord> out = new ArrayList<>();
    // Five for trace A (successful checkout)
    String tA = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // hex expected after mapping
    out.add(new LogIngestRecord(base + 1, tA, 20, "GET /api/catalog/search?q=running+shoes user=u123"));
    out.add(new LogIngestRecord(base + 2, tA, 10, "PricingEngine applied discount code=SPRING10 order=1001"));
    out.add(new LogIngestRecord(base + 3, tA, 30, "Inventory low for sku=SHOE-RED-42 remaining=2"));
    out.add(new LogIngestRecord(base + 4, tA, 20, "Payment authorized order=1001 amount=79.99 provider=stripe"));
    out.add(new LogIngestRecord(base + 5, tA, 20, "Order confirmed order=1001 user=u123"));
    // Five for trace B (failed checkout)
    String tB = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    out.add(new LogIngestRecord(base + 6, tB, 20, "GET /api/catalog/product/SHOE-BLUE-41 user=u999"));
    out.add(new LogIngestRecord(
        base + 7,
        tB,
        40,
        "Payment authorization failed order=2002 amount=129.00 provider=stripe code=card_declined"));
    out.add(new LogIngestRecord(base + 8, tB, 30, "Cart abandoned user=u999 after=2m"));
    out.add(new LogIngestRecord(base + 9, tB, 40, "Order creation failed order=2002 cause=payment_error"));
    out.add(new LogIngestRecord(base + 10, tB, 20, "Retry scheduled for payment order=2002 in=5m"));
    return out;
  }

  /** Backwards-compatible: builds a LogPage using the full corpus. */
  public LogPage buildTestPage() {
    LogPage page =
        LogPage.builder().expectedInsertions(1000).maxRangeMs(10_000L).maxSizeBytes(10_000).build();
    for (var rec : getIndividualRecords()) {
      page.append(rec);
    }
    return page;
  }

  public void checkQueryProcessorUsualRoute(
      LogsQueryProcessor qp, String stream, QueryConfig qc) throws Exception {

    long start = testStart - 1000;
    long end = testStart + 1000;
    var warn = qp.getLogs(stream, start, end, new LevelPageFilter(30), qc);
    assertEquals(2, warn.size());

    var tA =
        qp.getLogs(
            stream,
            start,
            end,
            new LogPageTraceFilter("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
            qc);
    assertEquals(5, tA.size());

    var failed = qp.getLogs(stream, start, end, new RegexPageFilter("failed"), qc);
    assertEquals(2, failed.size());
  }
}
