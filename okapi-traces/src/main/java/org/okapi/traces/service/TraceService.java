package org.okapi.traces.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.okapi.traces.model.Span;
import org.okapi.traces.sampler.SamplingStrategy;
import org.okapi.traces.storage.TraceRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TraceService {

  private final TraceRepository traceRepository;
  private final SamplingStrategy samplingStrategy;

  public List<Span> getSpans(String traceId, String tenant, String app) {
    return traceRepository.getSpansByTraceId(traceId, tenant, app);
  }

  public Optional<Span> getSpanById(String spanId, String tenant, String app) {
    return traceRepository.getSpanById(spanId, tenant, app);
  }

  public List<Span> listByDuration(
      String tenant, String app, long startMillis, long endMillis, int limit) {
    return traceRepository.listSpansByDuration(tenant, app, startMillis, endMillis, limit);
  }

  public int ingestOtelJson(JsonNode root, String tenant, String app) {
    if (root == null || !root.has("resourceSpans")) return 0;
    List<Span> toWrite = new ArrayList<>();
    for (JsonNode rs : root.path("resourceSpans")) {
      for (JsonNode ss : rs.path("scopeSpans")) {
        for (JsonNode sp : ss.path("spans")) {
          String traceId = text(sp, "traceId");
          if (traceId == null || traceId.isEmpty()) continue;
          // Head sampling on trace
          if (!samplingStrategy.sample(traceId)) continue;

          String spanId = text(sp, "spanId");
          String parentSpanId = text(sp, "parentSpanId");
          String name = text(sp, "name");
          String kind = kindText(sp);
          long startNanos = asLong(sp, "startTimeUnixNano");
          long endNanos = asLong(sp, "endTimeUnixNano");
          long durationMillis = Math.max(0L, (endNanos - startNanos) / 1_000_000L);
          Instant start = Instant.ofEpochMilli(startNanos / 1_000_000L);
          Instant end = Instant.ofEpochMilli(endNanos / 1_000_000L);

          Map<String, String> attributes = extractStringAttributes(sp.path("attributes"));
          // Status
          String statusCode = null;
          String statusMessage = null;
          if (sp.has("status")) {
            statusCode = text(sp.path("status"), "code");
            statusMessage = text(sp.path("status"), "message");
          }

          Span s =
              Span.builder()
                  .tenantId(tenant)
                  .appId(app)
                  .traceId(traceId)
                  .spanId(spanId)
                  .parentSpanId(parentSpanId)
                  .name(name)
                  .startTime(start)
                  .endTime(end)
                  .durationMillis(durationMillis)
                  .kind(kind)
                  .statusCode(statusCode)
                  .statusMessage(statusMessage)
                  .attributes(attributes)
                  .build();
          toWrite.add(s);
        }
      }
    }
    traceRepository.saveBatch(toWrite);
    return toWrite.size();
  }

  private static String text(JsonNode n, String field) {
    JsonNode v = n.get(field);
    if (v == null || v.isNull()) return null;
    return v.asText();
  }

  private static long asLong(JsonNode n, String field) {
    JsonNode v = n.get(field);
    if (v == null || v.isNull()) return 0L;
    try {
      // OTLP JSON represents nanos as strings
      return Long.parseLong(v.asText());
    } catch (NumberFormatException e) {
      return v.asLong(0L);
    }
  }

  private static String kindText(JsonNode sp) {
    JsonNode kindNode = sp.get("kind");
    if (kindNode == null) return null;
    return kindNode.asText();
  }

  private static Map<String, String> extractStringAttributes(JsonNode attrsNode) {
    Map<String, String> out = new HashMap<>();
    if (attrsNode == null || !attrsNode.isArray()) return out;
    for (JsonNode attr : attrsNode) {
      String k = text(attr, "key");
      if (k == null) continue;
      JsonNode v = attr.get("value");
      if (v == null) continue;
      // value is a union in OTLP JSON; pick string/bool/int/double as text
      String str =
          Optional.ofNullable(v.get("stringValue"))
              .map(JsonNode::asText)
              .orElseGet(
                  () ->
                      Optional.ofNullable(v.get("boolValue"))
                          .map(JsonNode::asText)
                          .orElseGet(
                              () ->
                                  Optional.ofNullable(v.get("intValue"))
                                      .map(JsonNode::asText)
                                      .orElseGet(
                                          () ->
                                              Optional.ofNullable(v.get("doubleValue"))
                                                  .map(JsonNode::asText)
                                                  .orElse(null))));
      if (str != null) out.put(k, str);
    }
    return out;
  }
}
