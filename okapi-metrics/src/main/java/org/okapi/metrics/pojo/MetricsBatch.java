package org.okapi.metrics.pojo;

import java.util.Map;

public record MetricsBatch(String name, Map<String, String> tags, Long timestamps, Double value) {}
