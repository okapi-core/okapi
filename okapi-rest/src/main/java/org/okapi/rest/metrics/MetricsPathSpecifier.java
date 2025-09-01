package org.okapi.rest.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
@AllArgsConstructor
public class MetricsPathSpecifier {
    String name;
    Map<String, String> tags;
}
