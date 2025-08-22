package org.okapi.metrics;

import org.okapi.metrics.stats.RolledUpStatistics;
import lombok.Value;

import java.util.List;

@Value
public class CondensedReading {
    List<Integer> ts;
    List<RolledUpStatistics> vals;
}
