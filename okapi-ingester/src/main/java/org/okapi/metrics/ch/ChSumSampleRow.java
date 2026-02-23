package org.okapi.metrics.ch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@AllArgsConstructor
@Getter
@Builder
public class ChSumSampleRow {
    String resource;
    String metricName;
    Map<String, String> tags;
    long tsStart;
    long tsEnd;
    long value;
    CH_SUM_TYPE sumType;
}
