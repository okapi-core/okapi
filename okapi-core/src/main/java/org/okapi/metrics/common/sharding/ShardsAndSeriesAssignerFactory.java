package org.okapi.metrics.common.sharding;

import java.util.List;

public interface ShardsAndSeriesAssignerFactory {
    ShardsAndSeriesAssigner makeAssigner(int nShards, List<String> nodes);
}
