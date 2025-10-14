package org.okapi.traces.query;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;

final class TraceSpanUtils {
  private TraceSpanUtils() {}

  static boolean spanOverlaps(Span sp, long start, long end) {
    long s = sp.getStartTimeUnixNano() / 1_000_000L;
    long e = sp.getEndTimeUnixNano() / 1_000_000L;
    return s <= end && start <= e;
  }

  static List<?> getScopeOrInstrumentationSpans(ResourceSpans rs) {
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
  static List<Span> getSpansFromScope(Object scope) {
    try {
      var m = scope.getClass().getMethod("getSpansList");
      return (List<Span>) m.invoke(scope);
    } catch (Exception e) {
      return List.of();
    }
  }

  static String anyValueToString(AnyValue v) {
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

  static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) sb.append(String.format("%02x", b));
    return sb.toString();
  }
}

