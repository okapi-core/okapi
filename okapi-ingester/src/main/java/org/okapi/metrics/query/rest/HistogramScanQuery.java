package org.okapi.metrics.query.rest;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class HistogramScanQuery {
    String name;
    Map<String, String> tags;
    long start;
    long end;
}
