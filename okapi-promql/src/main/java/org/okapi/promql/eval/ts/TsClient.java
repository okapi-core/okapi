package org.okapi.promql.eval.ts;


import org.okapi.Statistics;

import java.util.Map;

public interface TsClient {
    // Your provided API:
    Map<Long, Statistics> get(String name, Map<String,String> tags, RESOLUTION res, long startMs, long endMs);
}
