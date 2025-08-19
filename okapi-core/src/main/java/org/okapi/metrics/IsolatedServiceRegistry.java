package org.okapi.metrics;

import com.google.common.collect.Sets;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.pojo.*;

@Slf4j
@AllArgsConstructor
public class IsolatedServiceRegistry implements ServiceRegistry {

  public IsolatedServiceRegistry(Node node) {
    this.node = node;
    this.state = NodeState.NODE_CREATED;
    this.heartBeat = System.currentTimeMillis();
  }

  Node node;
  NodeState state;
  long heartBeat;

  @Override
  public void registerMetricProcessor() throws Exception {
    log.info("{} No need, I am isolated.", node.id());
  }

  @Override
  public Node getSelf() throws Exception {
    return node;
  }

  @Override
  public void setSelfState(NodeState state) throws Exception {
    this.state = state;
  }

  @Override
  public Optional<List<String>> listActiveNodes() throws Exception {
    return Optional.of(Collections.singletonList(node.id()));
  }

  @Override
  public Set<String> listRegisteredMetricNodes() throws Exception {
    return Sets.newHashSet(node.id());
  }

  @Override
  public Node getNode(String id) throws Exception {
    if (!id.equals(node.id())) {
      log.error("This shouldn't be called {}", id);
    }
    return node;
  }

  @Override
  public void safelyUpdateClusterOpState(String id, TWO_PHASE_STATE state) throws Exception {
    log.error("safelyUpdateClusterOpState shouldn't be called");
  }

  @Override
  public void safelyUpdateNodes(List<String> nodes) throws Exception {
    log.error("safelyUpdateNodes shouldn't be called");
  }

  @Override
  public Optional<ClusterChangeOp> clusterChangeOp() throws Exception {
    return Optional.empty();
  }

  @Override
  public Optional<ClusterConfig> oldClusterConfig() throws Exception {
    return Optional.empty();
  }

  @Override
  public ClusterConfig latestClusterConfig() throws Exception {
    return new ClusterConfig("default", listActiveNodes().get());
  }

  @Override
  public void writeHeartBeat() throws Exception {
    log.debug("Recording hearbeat to console.");
    heartBeat = System.currentTimeMillis();
  }

  @Override
  public Optional<Long> readHeartBeat(String node) throws Exception {
    return Optional.of(heartBeat);
  }

  @Override
  public void incFailedHeartBeatCount(String nodeId) throws Exception {
    checkNode("incFailedHeartBeatCount", nodeId);
  }

  @Override
  public void decFailedHeartBeatCount(String nodeId) throws Exception {
    checkNode("decFailedHeartBeatCount", nodeId);
  }

  @Override
  public int getFailedHeartBeatCount(String nodeId) throws Exception {
    return 0;
  }

  @Override
  public void safelySetUnhealthyNodes(Collection<String> nodes) throws Exception {
    log.error("Should not happen.");
  }

  @Override
  public List<String> getUnhealthyNodes() throws Exception {
    return List.of();
  }

  public void checkNode(String ctx, String nodeId) {
    if (!nodeId.equals(node.id())) {
      log.error("Got op for a different node, this is an isolated node.");
    }
  }
}
