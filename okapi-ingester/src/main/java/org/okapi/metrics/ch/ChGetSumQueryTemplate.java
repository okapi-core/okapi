package org.okapi.metrics.ch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Getter
@Builder
public class ChGetSumQueryTemplate {
    String table;
    String resource;
    String metric;
    Map<String, String> tags;
    String histoType;
    long ts;
    long te;
}
