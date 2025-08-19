package org.okapi.metrics.query;


import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;

import java.util.List;

public class QueryRecords {
    public record Slice(String series, long from, long to, RES_TYPE resolution, AGG_TYPE aggregation){};
    public record QueryResult(String name, List<Long> timestamps, List<Float> values){};
}
