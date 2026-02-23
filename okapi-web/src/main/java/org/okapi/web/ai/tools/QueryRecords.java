/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools;

import java.util.List;
import java.util.TreeMap;

public class QueryRecords {

  public record SpanQueryResult(
      String traceId,
      String spanId,
      String parentSpanId,
      String operationName,
      long startTime,
      long endTime,
      TreeMap<String, String> tags,
      TreeMap<String, Object> attributes) {}

  public record SpanBatch(List<SpanQueryResult> results) {}
}
