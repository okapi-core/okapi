package org.okapi.metrics.io;

import com.google.common.hash.BloomFilter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MetricsPageMetadataSnapshot {
    BloomFilter<Integer> metricNameTrigrams;
    BloomFilter<Integer> tagPatternTrigrams;
}
