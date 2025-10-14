package org.okapi.traces.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.okapi.traces.model.OkapiSpan;
import org.okapi.traces.sampler.SamplingStrategy;
import org.okapi.traces.storage.TraceRepository;

class TraceServiceTest {

  static class InMemoryRepo implements TraceRepository {
    List<OkapiSpan> stored = new ArrayList<>();

    @Override
    public void saveBatch(List<OkapiSpan> okapiSpans) {
      stored.addAll(okapiSpans);
    }

    @Override
    public List<OkapiSpan> getSpansByTraceId(String traceId, String tenant) {
      return List.of();
    }

    @Override
    public Optional<OkapiSpan> getSpanById(String spanId, String tenant) {
      return Optional.empty();
    }

    @Override
    public List<OkapiSpan> listSpansByDuration(
        String tenant, long startMillis, long endMillis, int limit) {
      return List.of();
    }

    @Override
    public Map<String, Object> listTracesByWindow(String tenant, long startMillis, long endMillis) {
      return Map.of();
    }

    @Override
    public List<OkapiSpan> listErrorSpans(
        String tenant, long startMillis, long endMillis, int limit) {
      return List.of();
    }

    @Override
    public Map<Long, Map<String, Long>> spanHistogramByMinute(
        String tenant, long startMillis, long endMillis) {
      return Map.of();
    }
  }

  static class AlwaysSample implements SamplingStrategy {
    @Override
    public boolean sample(String traceId) {
      return true;
    }
  }

  @Test
  void ingestOtelJson_parsesSpansAndEvents() throws Exception {
    String json =
        """
        {
          "resourceSpans": [
            {
              "scopeSpans": [
                {
                  "okapiSpans": [
                    {
                      "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
                      "spanId": "00f067aa0ba902b7",
                      "parentSpanId": "1111111111111111",
                      "name": "GET /api",
                      "kind": "SPAN_KIND_SERVER",
                      "startTimeUnixNano": "1700000000000000000",
                      "endTimeUnixNano": "1700000005000000000",
                      "attributes": [{"key": "http.method", "value": {"stringValue": "GET"}}],
                      "events": [
                        {"name": "e1", "timeUnixNano": "1700000001000000000", "attributes": [{"key":"k","value":{"stringValue":"v"}}]}
                      ],
                      "status": {"code": "STATUS_CODE_OK", "message": "OK"}
                    }
                  ]
                }
              ]
            }
          ]
        }
        """;
    InMemoryRepo repo = new InMemoryRepo();
    TraceService svc = new TraceService(repo, new AlwaysSample());
    int n = svc.ingestOtelJson(json, "tenantA");
    assertEquals(1, n);
    assertEquals(1, repo.stored.size());
    OkapiSpan s = repo.stored.getFirst();
    assertEquals("tenantA", s.getTenantId());
    assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", s.getTraceId());
    assertEquals("00f067aa0ba902b7", s.getSpanId());
    assertEquals(5000L, s.getDurationMillis());
    assertNotNull(s.getAttributes());
    assertEquals("GET", s.getAttributes().get("http.method"));
    assertEquals(1, s.getEvents().size());
    assertEquals("e1", s.getEvents().getFirst().get("name"));
  }
}
