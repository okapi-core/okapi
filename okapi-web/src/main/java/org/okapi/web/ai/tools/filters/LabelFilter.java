package org.okapi.web.ai.tools.filters;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LabelFilter {
  Map<String, String> labels;
}
