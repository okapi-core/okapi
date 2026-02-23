package org.okapi.traces.testutil;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import java.nio.charset.StandardCharsets;

public final class OtelShortHands {
  private OtelShortHands() {}

  public static KeyValue keyValue(String key, String value) {
    return KeyValue.newBuilder()
        .setKey(key)
        .setValue(AnyValue.newBuilder().setStringValue(value))
        .build();
  }

  public static KeyValue keyValue(String key, int value) {
    return KeyValue.newBuilder()
        .setKey(key)
        .setValue(AnyValue.newBuilder().setIntValue(value))
        .build();
  }

  public static KeyValue keyValue(String key, boolean value) {
    return KeyValue.newBuilder()
        .setKey(key)
        .setValue(AnyValue.newBuilder().setBoolValue(value))
        .build();
  }

  public static ByteString utf8Bytes(String value) {
    return ByteString.copyFrom(value.getBytes(StandardCharsets.UTF_8));
  }
}
