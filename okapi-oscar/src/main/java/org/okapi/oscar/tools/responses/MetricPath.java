package org.okapi.oscar.tools.responses;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class MetricPath {
    String name;
    Map<String, String> tags;
}
