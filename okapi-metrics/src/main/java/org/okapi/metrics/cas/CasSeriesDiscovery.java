package org.okapi.metrics.cas;

import java.util.List;
import lombok.AllArgsConstructor;
import org.okapi.metrics.cas.dao.SearchHintDao;
import org.okapi.promql.eval.VectorData;
import org.okapi.promql.eval.ts.SeriesDiscovery;
import org.okapi.promql.parse.LabelMatcher;

@AllArgsConstructor
public class CasSeriesDiscovery implements SeriesDiscovery {
    SearchHintDao searchHintDao;
    @Override
    public List<VectorData.SeriesId> expand(String metricOrNull, List<LabelMatcher> matchers, long start, long end) {
        // add a pattern here and don't introduce more abstractions
        return null;
    }
}
