package org.okapi.metrics.fdb;

import org.okapi.promql.eval.VectorData;
import org.okapi.promql.eval.ts.SeriesDiscovery;
import org.okapi.promql.parse.LabelMatcher;

import java.util.List;

public class FdbSeriesDiscovery implements SeriesDiscovery {
    @Override
    public List<VectorData.SeriesId> expand(String metricOrNull, List<LabelMatcher> matchers) {
        throw new IllegalArgumentException();
    }
}
