package org.okapi.traces.api.dto;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SpanDtoMapper {
  private SpanDtoMapper() {}

  public static SpanQueryResponse toResponse(List<Span> spans) {
    List<SpanDto> list = spans.stream().map(SpanDtoMapper::toDto).collect(Collectors.toList());
    return SpanQueryResponse.builder().spans(list).build();
  }

  public static SpanDto toDto(Span sp) {
    return SpanDto.builder()
        .traceId(bytesToHex(sp.getTraceId().toByteArray()))
        .spanId(bytesToHex(sp.getSpanId().toByteArray()))
        .parentSpanId(nullIfEmpty(bytesToHex(sp.getParentSpanId().toByteArray())))
        .name(sp.getName())
        .kind(sp.getKind().name())
        .statusCode(sp.hasStatus() ? sp.getStatus().getCode().name() : null)
        .startTimeUnixNano(sp.getStartTimeUnixNano())
        .endTimeUnixNano(sp.getEndTimeUnixNano())
        .attributes(toAttributes(sp.getAttributesList()))
        .build();
  }

  private static Map<String, String> toAttributes(List<KeyValue> list) {
    Map<String, String> m = new LinkedHashMap<>();
    for (KeyValue kv : list) {
      m.put(kv.getKey(), anyValueToString(kv.getValue()));
    }
    return m;
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
      case VALUE_NOT_SET -> "";
    };
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) sb.append(String.format("%02x", b));
    return sb.toString();
  }

  private static String nullIfEmpty(String s) {
    return (s == null || s.isEmpty()) ? null : s;
  }
}

