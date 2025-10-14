package org.okapi.traces.service;

import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.okapi.traces.model.OkapiSpan;
import org.okapi.traces.page.BufferPoolManager;
import org.okapi.traces.sampler.SamplingStrategy;
import org.okapi.traces.storage.TraceRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TraceService {

  private final TraceRepository traceRepository;
  private final SamplingStrategy samplingStrategy;
  private final BufferPoolManager bufferPoolManager;

  public List<OkapiSpan> getSpans(String traceId, String tenant) {
    return traceRepository.getSpansByTraceId(traceId, tenant);
  }

  public Optional<OkapiSpan> getSpanById(String spanId, String tenant) {
    return traceRepository.getSpanById(spanId, tenant);
  }

  public List<OkapiSpan> listByDuration(
      String tenant, long startMillis, long endMillis, int limit) {
    return traceRepository.listSpansByDuration(tenant, startMillis, endMillis, limit);
  }

  public int ingestOtelJson(String json, String tenant) {
    try {
      var builder = ExportTraceServiceRequest.newBuilder();
      JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
      return ingestRequest(builder.build(), tenant);
    } catch (Exception e) {
      return 0;
    }
  }

  public int ingestOtelProtobuf(byte[] body, String tenant) {
    try {
      var req = ExportTraceServiceRequest.parseFrom(body);
      return ingestRequest(req, tenant);
    } catch (Exception e) {
      return 0;
    }
  }

  // New ingestion path: write raw OTLP payloads into BufferPoolManager pages per tenant/app
  public int ingestOtelProtobuf(byte[] body, String tenant, String application) {
    try {
      var req = ExportTraceServiceRequest.parseFrom(body);
      bufferPoolManager.consume(tenant, application, req);
      // count spans for accounting
      int count = 0;
      for (var rs : req.getResourceSpansList()) {
        try {
          var m = rs.getClass().getMethod("getScopeSpansList");
          var scopes = (java.util.List<?>) m.invoke(rs);
          for (Object scope : scopes) {
            var mm = scope.getClass().getMethod("getSpansList");
            count += ((java.util.List<?>) mm.invoke(scope)).size();
          }
        } catch (Exception ignored) {
        }
      }
      return count;
    } catch (Exception e) {
      return 0;
    }
  }

  private int ingestRequest(ExportTraceServiceRequest req, String tenant) {
    List<OkapiSpan> toWrite = new ArrayList<>();
    for (var rs : req.getResourceSpansList()) {
      for (Object ss : getScopeOrInstrumentationSpans(rs)) {
        for (io.opentelemetry.proto.trace.v1.Span sp : getSpansFromScope(ss)) {
          String traceId = bytesToHex(sp.getTraceId().toByteArray());
          if (!samplingStrategy.sample(traceId)) continue;
          String spanId = bytesToHex(sp.getSpanId().toByteArray());
          String parentSpanId = bytesToHex(sp.getParentSpanId().toByteArray());
          String name = sp.getName();
          String kind = sp.getKind().name();
          long startNanos = sp.getStartTimeUnixNano();
          long endNanos = sp.getEndTimeUnixNano();
          long durationMillis = Math.max(0L, (endNanos - startNanos) / 1_000_000L);
          Instant start = Instant.ofEpochMilli(startNanos / 1_000_000L);
          Instant end = Instant.ofEpochMilli(endNanos / 1_000_000L);
          Map<String, String> attributes =
              sp.getAttributesList().stream()
                  .collect(
                      Collectors.toMap(
                          KeyValue::getKey, kv -> anyValueToString(kv.getValue()), (a, b) -> a));
          List<Map<String, String>> events =
              sp.getEventsList().stream()
                  .map(
                      ev -> {
                        Map<String, String> m = new HashMap<>();
                        m.put("name", ev.getName());
                        m.put("timeUnixNano", String.valueOf(ev.getTimeUnixNano()));
                        ev.getAttributesList()
                            .forEach(kv -> m.put(kv.getKey(), anyValueToString(kv.getValue())));
                        return m;
                      })
                  .toList();

          String statusCode = sp.getStatus().getCode().name();
          String statusMessage = sp.getStatus().getMessage();

          OkapiSpan s =
              OkapiSpan.builder()
                  .tenantId(tenant)
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
                  .events(events)
                  .build();
          toWrite.add(s);
        }
      }
    }
    traceRepository.saveBatch(toWrite);
    return toWrite.size();
  }

  // removed JSON tree helpers; using protobuf JsonFormat instead

  public Map<String, Object> listTracesByWindow(String tenant, long startMillis, long endMillis) {
    return traceRepository.listTracesByWindow(tenant, startMillis, endMillis);
  }

  public List<OkapiSpan> listErrorSpans(
      String tenant, long startMillis, long endMillis, int limit) {
    return traceRepository.listErrorSpans(tenant, startMillis, endMillis, limit);
  }

  public Map<Long, Map<String, Long>> spanHistogramByMinute(
      String tenant, long startMillis, long endMillis) {
    return traceRepository.spanHistogramByMinute(tenant, startMillis, endMillis);
  }

  private static String anyValueToString(AnyValue v) {
    return switch (v.getValueCase()) {
      case STRING_VALUE -> v.getStringValue();
      case BOOL_VALUE -> Boolean.toString(v.getBoolValue());
      case INT_VALUE -> Long.toString(v.getIntValue());
      case DOUBLE_VALUE -> Double.toString(v.getDoubleValue());
      case ARRAY_VALUE -> v.getArrayValue().getValuesList().toString();
      case KVLIST_VALUE -> v.getKvlistValue().getValuesList().toString();
      case BYTES_VALUE -> bytesToHex(v.getBytesValue().toByteArray());
      case VALUE_NOT_SET -> null;
    };
  }

  // Compatibility: support both ResourceSpans.getScopeSpansList() and
  // ResourceSpans.getInstrumentationLibrarySpansList() via reflection.
  private static List<?> getScopeOrInstrumentationSpans(
      io.opentelemetry.proto.trace.v1.ResourceSpans rs) {
    try {
      var m = rs.getClass().getMethod("getScopeSpansList");
      return (List<?>) m.invoke(rs);
    } catch (NoSuchMethodException e) {
      try {
        var m = rs.getClass().getMethod("getInstrumentationLibrarySpansList");
        return (List<?>) m.invoke(rs);
      } catch (Exception ex) {
        return List.of();
      }
    } catch (Exception e) {
      return List.of();
    }
  }

  @SuppressWarnings("unchecked")
  private static List<io.opentelemetry.proto.trace.v1.Span> getSpansFromScope(Object scope) {
    try {
      var m = scope.getClass().getMethod("getSpansList");
      return (List<io.opentelemetry.proto.trace.v1.Span>) m.invoke(scope);
    } catch (Exception e) {
      return List.of();
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
