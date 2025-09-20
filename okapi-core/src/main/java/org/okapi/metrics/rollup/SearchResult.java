package org.okapi.metrics.rollup;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
@EqualsAndHashCode
public class SearchResult {
    String tenantId;
    String name;
    Map<String, String> tags;
    String type;
}
