package org.okapi.metrics.common;

import org.okapi.metrics.common.pojo.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ServiceRegistry {
  /** Methods to read / report its own state * */
  void registerMetricProcessor() throws Exception;

  Node getSelf() throws Exception;

  void setSelfState(NodeState state) throws Exception;

  /** Methods to read data by other nodes, these should only be called by a lead * */
  Optional<List<String>> listActiveNodes() throws Exception;

  Set<String> listRegisteredMetricNodes() throws Exception;

  Node getNode(String id) throws Exception;

  void safelyUpdateClusterOpState(String id, TWO_PHASE_STATE state) throws Exception;

  /** Methods related to node scale up */
  void safelyUpdateNodes(List<String> nodes) throws Exception;

  Optional<ClusterChangeOp> clusterChangeOp() throws Exception;

  Optional<ClusterConfig> oldClusterConfig() throws Exception;

  ClusterConfig latestClusterConfig() throws Exception;

  /** Read / check healthchecks * */
  void writeHeartBeat() throws Exception;

  Optional<Long> readHeartBeat(String node) throws Exception;

  void incFailedHeartBeatCount(String nodeId) throws Exception;

  void decFailedHeartBeatCount(String nodeId) throws Exception;

  int getFailedHeartBeatCount(String nodeId) throws Exception;

  //  For the leader to mark unhealthy nodes: this is for coordination during shard movement.
  void safelySetUnhealthyNodes(Collection<String> nodes) throws Exception;

  List<String> getUnhealthyNodes() throws Exception;
}
