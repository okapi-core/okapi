package org.okapi.datagen.spans;

import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class TraceContext {
  SystemState state;
  byte[] traceId;
  Map<String, List<Span>> spansByService;
}
