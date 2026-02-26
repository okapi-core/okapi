package org.okapi.metrics.ch.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.okapi.ch.AbstractChTemplate;

import java.util.Map;

@AllArgsConstructor
@Getter
@Builder
public class ChGetExemplarTemplate extends AbstractChTemplate {
  String fqTable;
  String metricName;
  Map<String, String> tags;
  long tsNanosStart;
  long tsNanosEnd;
}
