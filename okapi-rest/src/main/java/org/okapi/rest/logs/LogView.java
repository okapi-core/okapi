package org.okapi.rest.logs;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Builder
@Value
@Getter
public class LogView {
  long tsMillis;
  int level;
  String body;
  String traceId;
  String docId;
  String service;
}
