package org.okapi.traces.model;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Span {
  private String tenantId;
  private String appId;

  private String traceId;
  private String spanId;
  private String parentSpanId;
  private String name;
  private Instant startTime;
  private Instant endTime;
  private long durationMillis;

  private String kind; // SERVER/CLIENT/INTERNAL etc.
  private String statusCode;
  private String statusMessage;

  private Map<String, String> attributes; // flattened string attributes for quick metadata viewing
}
