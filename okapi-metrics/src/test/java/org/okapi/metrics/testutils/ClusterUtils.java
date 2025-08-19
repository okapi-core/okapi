package org.okapi.metrics.testutils;

import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.pojo.TWO_PHASE_STATE;

import java.util.List;

public class ClusterUtils {

  public static void scaleUpTo(ServiceRegistry registry, List<String> nodes) throws Exception {
    registry.safelyUpdateNodes(nodes);
    var updateOpId = registry.clusterChangeOp().get().opId();
    registry.safelyUpdateClusterOpState(updateOpId, TWO_PHASE_STATE.DONE);
  }
}
