package org.okapi.metrics.rollup;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Getter
public class SearchResult {
    String tenantId;
    String name;
    Map<String, String> tags;
}
