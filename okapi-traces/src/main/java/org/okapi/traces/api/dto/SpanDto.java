package org.okapi.traces.api.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpanDto {
  private String traceId;
  private String spanId;
  private String parentSpanId; // nullable
  private String name;
  private String kind; // SpanKind name
  private String statusCode; // Status code name
  private long startTimeUnixNano;
  private long endTimeUnixNano;
  private Map<String, String> attributes; // flattened string attributes
}

