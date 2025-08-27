package org.okapi.metrics.sharding;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.clock.Clock;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.ZkResources;
import org.okapi.metrics.common.pojo.ClusterChangeOp;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.common.pojo.TWO_PHASE_STATE;

@Slf4j
@AllArgsConstructor
public class LeaderJobsImpl implements LeaderJobs {

  public static final Duration OP_TIMEOUT = Duration.of(30, ChronoUnit.MINUTES);
  public static final int FAILED_HEART_BEAT_COUNT_THRESHOLD = 10;

  ServiceRegistry serviceRegistry;
  HeartBeatChecker beatChecker;
  ZkResources zk;
  Clock clock;

  @Override
  public void checkFleetHealth() throws Exception {
    log.info("Possibly checking fleet health.");
    if (!zk.isLeader()) {
      log.info("Not leader, not my job.");
      return;
    }
    log.info("Checking fleet health.");

    var registeredNodes = serviceRegistry.listRegisteredMetricNodes();
    /**
     * If a node has been dead for a while it will be marked as dead. If a resharding operation was
     * ongoing while a node dies, a rollback command is issued.
     */
    var unhealthy = new HashSet<String>();
    for (var node : registeredNodes) {
      var heartBeat = serviceRegistry.readHeartBeat(node);
      if (heartBeat.isEmpty()) {
        serviceRegistry.incFailedHeartBeatCount(node);
      } else {
        var isHealthy = beatChecker.isHealthy(heartBeat.get());
        if (isHealthy) {
          serviceRegistry.decFailedHeartBeatCount(node);
        } else {
          serviceRegistry.incFailedHeartBeatCount(node);
        }
      }
    }

    for (var node : registeredNodes) {
      var failedCount = serviceRegistry.getFailedHeartBeatCount(node);
      if (failedCount > FAILED_HEART_BEAT_COUNT_THRESHOLD) {
        unhealthy.add(node);
      }
    }
    serviceRegistry.safelySetUnhealthyNodes(unhealthy);
  }

  @Override
  public void checkShardMovementStatus() throws Exception {
    log.debug("Possibly checking scaling status.");
    if (!zk.isLeader()) {
      log.debug("Not leader, not my job.");
      return;
    }
    log.debug("Checking scaling status.");
    var maybeClusterChangeOp = serviceRegistry.clusterChangeOp();
    if (maybeClusterChangeOp.isEmpty()) {
      return;
    }

    var clusterUpdateOp = maybeClusterChangeOp.get();
    var participants = clusterUpdateOp.nodes();
    if (clusterUpdateOp.state() == TWO_PHASE_STATE.DONE) {
      log.debug("Nothing to do, scaling is done");
      return;
    }
    if (clusterUpdateOp.state() == TWO_PHASE_STATE.FAILED) {
      log.debug("Nothing to do, scaling has failed.");
      return;
    }

    // If sharding doesn't transition to done within a certain
    if (clusterConfigUpdateTimeout(clusterUpdateOp)) {
      log.debug("Scaling timeout, rolling back.");
      serviceRegistry.safelyUpdateClusterOpState(clusterUpdateOp.opId(), TWO_PHASE_STATE.ROLLBACK);
    }

    if (clusterUpdateOp.state() == TWO_PHASE_STATE.START) {
      log.debug("Scaling has starting, sending prepare command.");
      serviceRegistry.safelyUpdateClusterOpState(clusterUpdateOp.opId(), TWO_PHASE_STATE.PREPARE);
    }

    // check if commit phase can be triggered
    else if (clusterUpdateOp.state() == TWO_PHASE_STATE.PREPARE
        && allNodesHaveState(participants, NodeState.SHARD_CHECKPOINTS_UPLOADED)) {
      log.debug("Nodes are done preparing, moving to commit phase.");
      serviceRegistry.safelyUpdateClusterOpState(clusterUpdateOp.opId(), TWO_PHASE_STATE.COMMIT);
    }

    // else if we are stage commit and all nodes have applied their checkpoints
    else if (clusterUpdateOp.state() == TWO_PHASE_STATE.COMMIT
        && allNodesHaveState(participants, NodeState.SHARD_CHECKPOINTS_APPLIED)) {
      /** Check if sharding can be marked as done */
      log.debug("Nodes are done comitting, marking this op as done.");
      serviceRegistry.safelyUpdateClusterOpState(clusterUpdateOp.opId(), TWO_PHASE_STATE.DONE);
    }

    // if we are at stage rollback and all nodes have rolled back their states
    //    if sharding is stuck in stage rollback the cluster needs to be manually repaired
    else if (clusterUpdateOp.state() == TWO_PHASE_STATE.ROLLBACK
        && allNodesHaveState(participants, NodeState.ROLLED_BACK)) {
      log.debug("Nodes are done rolling back, marking this op as failed.");
      serviceRegistry.safelyUpdateClusterOpState(clusterUpdateOp.opId(), TWO_PHASE_STATE.FAILED);
    }
  }

  public boolean clusterConfigUpdateTimeout(ClusterChangeOp shardOp) {
    return clock.currentTimeMillis() - shardOp.startTime() > OP_TIMEOUT.toMillis();
  }

  public boolean allNodesHaveState(List<String> nodes, NodeState state) throws Exception {
    log.info("Checking state of participants {}", nodes);
    int count = 0;
    for (var node : nodes) {
      count += (state == serviceRegistry.getNode(node).state()) ? 1 : 0;
    }
    return count == nodes.size();
  }
}
