package org.okapi.traces.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.Range;
import com.apple.foundationdb.tuple.Tuple;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.okapi.traces.model.Span;

/**
 * Integration tests for TraceRepository.
 *
 * These tests require a real FoundationDB instance reachable via the default cluster file.
 * If you want to skip these tests locally, set env var OKAPI_TRACES_IT to anything other than "1".
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "OKAPI_TRACES_IT", matches = "1")
public class TraceRepositoryTests {

  private TraceRepository repo;
  private Database db;

  private String tenant;
  private String app;

  @BeforeAll
  void setupAll() {
    // Open a real FDB connection to assert environment readiness
    FDB fdb = FDB.selectAPIVersion(TraceRepository.API_VERSION);
    this.db = fdb.open();

    this.repo = new TraceRepository(new ObjectMapper());
    this.repo.init();
  }

  @BeforeEach
  void setup() {
    this.tenant = "it-" + UUID.randomUUID();
    this.app = "app-" + UUID.randomUUID();
  }

  @AfterEach
  void cleanup() {
    // Clear only keys for our random tenant/app under all subspaces used by repository
    byte[] trPrefix = Tuple.from("tr", tenant, app).pack();
    byte[] sidPrefix = Tuple.from("sid", tenant, app).pack();
    byte[] durPrefix = Tuple.from("dur", tenant, app).pack();
    db.run(tr -> {
      tr.clear(Range.startsWith(trPrefix));
      tr.clear(Range.startsWith(sidPrefix));
      tr.clear(Range.startsWith(durPrefix));
      return null;
    });
  }

  @Test
  void saveAndFetchByTraceId_andBySpanId() {
    String traceId = "trace-1";

    Instant base = nowRoundedToHour().toInstant().plusSeconds(5);
    Span s1 = span(tenant, app, traceId, "s-1", null, base, base.plusMillis(100));
    Span s2 = span(tenant, app, traceId, "s-2", "s-1", base.plusMillis(1_000), base.plusMillis(2_000));
    Span s3 = span(tenant, app, traceId, "s-3", "s-1", base.plusMillis(2_500), base.plusMillis(2_900));

    repo.saveBatch(List.of(s1, s2, s3));

    var spans = repo.getSpansByTraceId(traceId, tenant, app);
    assertEquals(3, spans.size(), "Should return all spans for the trace");
    var ids = spans.stream().map(Span::getSpanId).collect(Collectors.toSet());
    assertTrue(ids.containsAll(Set.of("s-1", "s-2", "s-3")));

    var byId = repo.getSpanById("s-2", tenant, app);
    assertTrue(byId.isPresent(), "Span s-2 should be found by span-id index");
    assertEquals("s-2", byId.get().getSpanId());
  }

  @Test
  void listByDuration_respectsOrderingAndTenantAndWindow() {
    String traceA = "trace-A";
    String traceB = "trace-B";

    // Same hour bucket for simplicity
    ZonedDateTime hr = nowRoundedToHour();
    Instant t0 = hr.toInstant().plusSeconds(10);
    // Durations: 3000, 1500, 500
    Span a1 = span(tenant, app, traceA, "a1", null, t0, t0.plusMillis(3_000));
    Span a2 = span(tenant, app, traceA, "a2", "a1", t0.plusMillis(10), t0.plusMillis(1_510));
    Span b1 = span(tenant, app, traceB, "b1", null, t0.plusMillis(20), t0.plusMillis(520));

    // Different app/tenant noise (should be ignored)
    Span otherApp = span(tenant, app + "-other", "trace-C", "x1", null, t0, t0.plusMillis(10_000));
    Span otherTenant = span(tenant + "-other", app, "trace-D", "y1", null, t0, t0.plusMillis(10_000));

    repo.saveBatch(List.of(a1, a2, b1, otherApp, otherTenant));

    long start = hr.toInstant().toEpochMilli();
    long end = hr.plusMinutes(30).toInstant().toEpochMilli();
    var spans = repo.listSpansByDuration(tenant, app, start, end, 10);

    // Only 3 for our tenant/app
    assertEquals(3, spans.size(), "Should include only our tenant/app spans");

    // Ordered by duration desc: a1 (3000), a2 (≈1500), b1 (≈500)
    assertEquals("a1", spans.get(0).getSpanId());
    assertEquals("a2", spans.get(1).getSpanId());
    assertEquals("b1", spans.get(2).getSpanId());

    // Outside of window should exclude
    long narrowStart = hr.plusMinutes(1).toInstant().toEpochMilli();
    long narrowEnd = hr.plusMinutes(1).plusSeconds(1).toInstant().toEpochMilli();
    var narrow = repo.listSpansByDuration(tenant, app, narrowStart, narrowEnd, 10);
    assertTrue(narrow.isEmpty(), "Narrow window without spans should be empty");
  }

  private static ZonedDateTime nowRoundedToHour() {
    return Instant.now().atZone(ZoneOffset.UTC).withMinute(0).withSecond(0).withNano(0);
  }

  private static Span span(
      String tenant,
      String app,
      String traceId,
      String spanId,
      String parentSpanId,
      Instant start,
      Instant end) {
    return Span.builder()
        .tenantId(tenant)
        .appId(app)
        .traceId(traceId)
        .spanId(spanId)
        .parentSpanId(parentSpanId)
        .name("op-" + spanId)
        .startTime(start)
        .endTime(end)
        .durationMillis(Math.max(0, end.toEpochMilli() - start.toEpochMilli()))
        .kind("INTERNAL")
        .statusCode("OK")
        .attributes(Map.of("k", "v"))
        .build();
  }
}
