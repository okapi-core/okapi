package org.okapi.metrics.cas;

import org.okapi.promql.eval.VectorData;
import org.okapi.promql.eval.ts.SeriesDiscovery;
import org.okapi.promql.parse.LabelMatcher;

import java.util.List;

public class CasSeriesDiscovery implements SeriesDiscovery {
    @Override
    public List<VectorData.SeriesId> expand(String metricOrNull, List<LabelMatcher> matchers, long start, long end) {
        return null;
    }
}
