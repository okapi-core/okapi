package org.okapi.metrics.cas;

import java.util.Map;
import org.okapi.metrics.pojos.results.Scan;
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.promql.eval.ts.TsClient;

public class CasTsClient implements TsClient {
    String tenantId;

    @Override
    public Scan get(String name, Map<String, String> tags, RESOLUTION res, long startMs, long endMs) {
        throw new IllegalArgumentException();
    }
}
