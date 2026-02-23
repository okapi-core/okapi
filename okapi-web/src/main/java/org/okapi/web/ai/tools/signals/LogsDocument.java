package org.okapi.web.ai.tools.signals;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LogsDocument {
  String content;
  long timestamp;
  String level;
}
