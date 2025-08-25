package org.okapi.metrics;

import lombok.Value;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.stats.Statistics;

@Value
public class WriteBackRequest {
    MetricsContext context;
    int shard;
    String key;
    Statistics statistics;
}
