package org.okapi.web.ai.tools.params;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.web.ai.tools.filters.LabelFilter;

@AllArgsConstructor
@Getter
public class SpanQuery {
  long startTime;
  long endTime;
  String service;
  String traceId;
  LabelFilter labelFilter;
}
