package org.okapi.datagen.spans;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;

public final class OtelShorthand {
  private OtelShorthand() {}

  public static KeyValue kv(String key, String value) {
    return KeyValue.newBuilder()
        .setKey(key)
        .setValue(AnyValue.newBuilder().setStringValue(value).build())
        .build();
  }

  public static KeyValue kvInt(String key, int value) {
    return KeyValue.newBuilder()
        .setKey(key)
        .setValue(AnyValue.newBuilder().setIntValue(value).build())
        .build();
  }

  public static KeyValue kvDouble(String key, double value){
    return KeyValue.newBuilder()
            .setKey(key)
            .setValue(AnyValue.newBuilder().setDoubleValue(value).build())
            .build();
  }
}
