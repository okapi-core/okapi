package org.okapi.metrics.service.runnables;

import org.okapi.exceptions.ExceptionUtils;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.pojo.TWO_PHASE_STATE;
import org.okapi.metrics.service.ExponentialBackoffCalculator;
import org.okapi.metrics.service.Shardable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ClusterChangeListener implements Runnable {
  ServiceRegistry serviceRegistry;
  ScheduledExecutorService scheduledExecutorService;
  Shardable shardable;

  @Override
  public void run() {
    try {
      runOnce();
    } catch (Exception e) {
      log.error("Running failed due to {}", ExceptionUtils.debugFriendlyMsg(e));
    } finally {
      // randomly wait between 5-10s to check state again
      var waitTime = ExponentialBackoffCalculator.jitteryWait(5_000, 10_000);
      scheduledExecutorService.schedule(this, waitTime, TimeUnit.MILLISECONDS);
    }
  }

  protected void runOnce() throws Exception {
    log.debug("Checking for cluster change.");
    var maybeClusterChangeOp = serviceRegistry.clusterChangeOp();
    if (maybeClusterChangeOp.isEmpty()) {
      log.debug("No changes.");
      return;
    }
    var clusterChangeOpState = maybeClusterChangeOp.get().state();
    if (clusterChangeOpState == TWO_PHASE_STATE.PREPARE) {
      log.debug("Preparing for cluster update.");
      shardable.onShardMovePrepare();
    } else if (clusterChangeOpState == TWO_PHASE_STATE.COMMIT) {
      log.debug("Committing changes for cluster update.");
      shardable.onShardMoveCommit();
    } else if (clusterChangeOpState == TWO_PHASE_STATE.ROLLBACK) {
      log.debug("Rolling back cluster update.");
      shardable.onShardMoveRollback();
    }
  }
}
