package org.okapi.oscar.tools;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.okapi.ingester.client.IngesterClient;
import org.okapi.oscar.tools.responses.FilterContribution;
import org.okapi.rest.traces.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FilterContributionToolTest {

  static class FakeIngesterClient extends IngesterClient {
    private static final Map<String, Long> STRING_COUNTS =
        Map.of("http.method", 101L, "db.system", 102L);
    private static final Map<String, Long> NUMBER_COUNTS =
        Map.of("http.status_code", 201L, "duration.ms", 202L);

    FakeIngesterClient() {
      super("http://localhost", new OkHttpClient(), null);
    }

    @Override
    public SpanQueryV2SummaryResponse getResultsSummary(SpanQueryV2Request request) {
      long count = resolveCount(request);
      return SpanQueryV2SummaryResponse.builder().count(count).build();
    }

    private long resolveCount(SpanQueryV2Request request) {
      if (request.getStringAttributesFilter() != null
          && !request.getStringAttributesFilter().isEmpty()) {
        var key = request.getStringAttributesFilter().get(0).getKey();
        return STRING_COUNTS.getOrDefault(key, 0L);
      }
      if (request.getNumberAttributesFilter() != null
          && !request.getNumberAttributesFilter().isEmpty()) {
        var key = request.getNumberAttributesFilter().get(0).getKey();
        return NUMBER_COUNTS.getOrDefault(key, 0L);
      }
      if (request.getTraceId() != null) {
        return 11L;
      }
      if (request.getSpanId() != null) {
        return 22L;
      }
      if (request.getKind() != null) {
        return 33L;
      }
      if (request.getDbFilters() != null) {
        return 44L;
      }
      if (request.getDurationFilter() != null) {
        return 55L;
      }
      if (request.getHttpFilters() != null) {
        return 66L;
      }
      if (request.getServiceFilter() != null) {
        return 77L;
      }
      if (request.getTimestampFilter() != null) {
        return 88L;
      }
      return 0L;
    }
  }

  @Test
  void populatesCountsForAllFilters() {
    var client = new FakeIngesterClient();
    var tool = new FilterContributionTool(client, 4, 4, 1000);
    var request =
        SpanQueryV2Request.builder()
            .traceId("trace-1")
            .spanId("span-1")
            .kind("SPAN_KIND_SERVER")
            .dbFilters(DbFilters.builder().system("postgresql").build())
            .durationFilter(DurationFilter.builder().durMinMillis(10).durMaxMillis(20).build())
            .httpFilters(HttpFilters.builder().httpMethod("GET").statusCode(200).build())
            .serviceFilter(ServiceFilter.builder().service("svc-a").peer("svc-b").build())
            .timestampFilter(TimestampFilter.builder().tsStartNanos(1L).tsEndNanos(2L).build())
            .stringAttributesFilter(
                List.of(
                    StringAttributeFilter.builder().key("http.method").value("GET").build(),
                    StringAttributeFilter.builder().key("db.system").value("postgresql").build()))
            .numberAttributesFilter(
                List.of(
                    NumberAttributeFilter.builder().key("http.status_code").value(200.0).build(),
                    NumberAttributeFilter.builder().key("duration.ms").value(123.0).build()))
            .build();

    FilterContribution result = tool.getFilterContributions(request);

    assertEquals(11L, result.getTraceIdFilterResultCount());
    assertEquals(22L, result.getSpanIdFilterResultCount());
    assertEquals(33L, result.getKindFilterCount());
    assertEquals(44L, result.getDbFiltersCount());
    assertEquals(55L, result.getDurationFilterCount());
    assertEquals(66L, result.getHttpFiltersCount());
    assertEquals(77L, result.getServiceFilterCount());
    assertEquals(88L, result.getTimestampFilterCount());
    assertEquals(Map.of("http.method", 101L, "db.system", 102L), result.getStringAttributesFilterCount());
    assertEquals(
        Map.of("http.status_code", 201L, "duration.ms", 202L),
        result.getNumberAttributesFilterCount());
  }

  @Test
  void leavesCountsNullWhenFiltersAreMissing() {
    var client = new FakeIngesterClient();
    var tool = new FilterContributionTool(client, 2, 2, 1000);
    var request =
        SpanQueryV2Request.builder()
            .traceId("trace-1")
            .stringAttributesFilter(
                List.of(StringAttributeFilter.builder().key("http.method").value("GET").build()))
            .build();

    FilterContribution result = tool.getFilterContributions(request);

    assertEquals(11L, result.getTraceIdFilterResultCount());
    assertNull(result.getSpanIdFilterResultCount());
    assertNull(result.getKindFilterCount());
    assertNull(result.getDbFiltersCount());
    assertNull(result.getDurationFilterCount());
    assertNull(result.getHttpFiltersCount());
    assertNull(result.getServiceFilterCount());
    assertNull(result.getTimestampFilterCount());
    assertEquals(Map.of("http.method", 101L), result.getStringAttributesFilterCount());
    assertNull(result.getNumberAttributesFilterCount());
  }
}
