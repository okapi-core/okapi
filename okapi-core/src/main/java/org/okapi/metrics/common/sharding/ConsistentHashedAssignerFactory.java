package org.okapi.metrics.common.sharding;

import java.util.List;

public class ConsistentHashedAssignerFactory implements ShardsAndSeriesAssignerFactory {
  @Override
  public ShardsAndSeriesAssigner makeAssigner(int nShards, List<String> nodes) {
    return new ConsistentHashBasedAssigner(nShards, nodes);
  }
}
