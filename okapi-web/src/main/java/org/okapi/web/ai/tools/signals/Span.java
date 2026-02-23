/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.signals;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder
public class Span {
  String spanId;
  String service;
  String traceId;
  int level;
  String parentSpanId;
  long start;
  long end;
  Map<String, Object> attributes;

  public Span(
      String spanId,
      String service,
      String traceId,
      int level,
      String parentSpanId,
      long start,
      long end,
      @Singular Map<String, Object> attributes) {
    this.spanId = spanId;
    this.service = service;
    this.traceId = traceId;
    this.level = level;
    this.parentSpanId = parentSpanId;
    this.start = start;
    this.end = end;
    this.attributes = attributes;
  }

  public Object getAttribute(String key) {
    return attributes.get(key);
  }
}
