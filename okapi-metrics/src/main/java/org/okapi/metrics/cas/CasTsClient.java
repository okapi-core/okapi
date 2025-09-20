package org.okapi.metrics.cas;

import org.okapi.Statistics;
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.promql.eval.ts.TsClient;

import java.util.Map;

public class CasTsClient implements TsClient {
    String tenantId;

    @Override
    public Map<Long, Statistics> get(String name, Map<String, String> tags, RESOLUTION res, long startMs, long endMs) {
        return Map.of();
    }
}
