package org.okapi.metrics.cas;

import org.okapi.Statistics;
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.promql.eval.ts.TimeseriesClient;

import java.util.Map;

public class CasTsClient implements TimeseriesClient {
    String tenantId;

    @Override
    public Map<Long, Statistics> get(String name, Map<String, String> tags, RESOLUTION res, long startMs, long endMs) {
        return Map.of();
    }
}
