package org.okapi.metrics.cas;

import org.okapi.metrics.common.MetricsPathParser;
import org.okapi.metrics.rollup.SearchResult;
import org.okapi.metrics.rollup.TsSearcher;

import java.util.List;

public class CasTsSearcher implements TsSearcher {
    @Override
    public List<SearchResult> search(String tenantId, String pattern, long start, long end) {
        return List.of();
    }

    @Override
    public List<MetricsPathParser.MetricsRecord> list(String tenantId, long start, long end) {
        return List.of();
    }
}
