package org.okapi.rest.search;

import lombok.*;

import java.util.Map;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@ToString
@Builder
public class MetricPath {
    String metric;
    Map<String, String> labels;
}
