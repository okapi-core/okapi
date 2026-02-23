package org.okapi.metrics.ch;



import java.util.Map;

public record ChSumSample(
    long tsStart,
    long tsEnd,
    long value,
    CH_SUM_TYPE sumType,
    Map<String, String> tags) {}
