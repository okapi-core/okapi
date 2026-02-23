/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.mappers;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class OtelToLogMapper {
  private OtelToLogMapper() {}

  public static int mapLevel(int severityNumber) {
    // Map many OTel severities to four buckets: 10/20/30/40
    // OTel: 1..4 TRACE, 5..8 DEBUG, 9..12 INFO, 13..16 WARN, 17..20 ERROR, 21..24 FATAL
    if (severityNumber <= 4) return 10; // treat TRACE as DEBUG for now
    if (severityNumber <= 8) return 10; // DEBUG
    if (severityNumber <= 12) return 20; // INFO
    if (severityNumber <= 16) return 30; // WARN
    return 40; // ERROR and FATAL
  }

  public static String anyValueToString(AnyValue v) {
    if (v == null) return "";
    return switch (v.getValueCase()) {
      case STRING_VALUE -> v.getStringValue();
      case BOOL_VALUE -> String.valueOf(v.getBoolValue());
      case INT_VALUE -> String.valueOf(v.getIntValue());
      case DOUBLE_VALUE -> String.valueOf(v.getDoubleValue());
      case ARRAY_VALUE -> v.getArrayValue().getValuesList().toString();
      case KVLIST_VALUE -> v.getKvlistValue().getValuesList().toString();
      case BYTES_VALUE -> v.getBytesValue().toString(StandardCharsets.UTF_8);
      case VALUE_NOT_SET -> "";
    };
  }

  public static String traceIdToHex(ByteString traceId) {
    if (traceId == null || traceId.isEmpty()) return "";
    byte[] b = traceId.toByteArray();
    StringBuilder sb = new StringBuilder(b.length * 2);
    for (byte value : b) sb.append(String.format(Locale.ROOT, "%02x", value));
    return sb.toString();
  }
}
