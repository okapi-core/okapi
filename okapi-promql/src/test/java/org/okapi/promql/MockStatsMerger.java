package org.okapi.promql;

import java.util.ArrayList;
import java.util.Collections;
import org.okapi.Statistics;
import org.okapi.promql.eval.ts.StatisticsMerger;

public class MockStatsMerger implements StatisticsMerger {
    @Override
    public Statistics merge(Statistics a, Statistics b) {
        var castA = (MockStatistics) a;
        var castB = (MockStatistics) b; // fixed
        var combined = new ArrayList<Float>(castA.getValues().size() + castB.getValues().size());
        combined.addAll(castA.getValues());
        combined.addAll(castB.getValues());
        return new MockStatistics(Collections.unmodifiableList(combined));
    }
}

