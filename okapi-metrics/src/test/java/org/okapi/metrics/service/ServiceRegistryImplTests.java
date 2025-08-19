package org.okapi.metrics.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.common.pojo.ShardConfig;
import org.okapi.metrics.common.pojo.TWO_PHASE_STATE;

public class ServiceRegistryImplTests {

  TestResourceFactory testResourceFactory;
  Node testNode;
  ShardConfig shardConfig;

  @BeforeEach
  public void setup() {
    testResourceFactory = new TestResourceFactory();
    shardConfig = new ShardConfig("opIdA", 3);
    testNode = testResourceFactory.makeNode("test-node");
  }

  @Test
  public void testRegisterMetricProcessor() throws Exception {
    var serviceRegistry = testResourceFactory.serviceRegistry(testNode);
    serviceRegistry.registerMetricProcessor();
    var nodes = serviceRegistry.listRegisteredMetricNodes();
    assert nodes.contains(testNode.id()) : "Node should be registered in the service registry";
  }

  @Test
  public void testGetMetadata() throws Exception {
    var serviceRegistry = testResourceFactory.serviceRegistry(testNode);
    serviceRegistry.registerMetricProcessor();
    var metadata = serviceRegistry.getNode(testNode.id());
    assert metadata.id().equals(testNode.id()) : "Metadata should match the registered node";
    assert metadata.ip().equals(testNode.ip()) : "IP should match the registered node";
    assert metadata.state() == testNode.state() : "State should match the registered node";
  }

  @Test
  public void testListActiveNodes() throws Exception {
    var serviceRegistry = testResourceFactory.serviceRegistry(testNode);
    serviceRegistry.registerMetricProcessor();
    var nodes = serviceRegistry.listRegisteredMetricNodes();
    assert nodes.size() == 1 : "There should be one node registered";
    assert nodes.contains(testNode.id()) : "Registered node should be in the list";
  }

  @Test
  public void testWriteHealthCheck() throws Exception {
    var serviceRegistry = testResourceFactory.serviceRegistry(testNode);
    serviceRegistry.registerMetricProcessor();
    serviceRegistry.writeHeartBeat();
    var heartBeat = serviceRegistry.readHeartBeat(testNode.id());
    assert heartBeat.isPresent() : "HeartBeat should be present after writing";
    assert heartBeat.get() > 0 : "HeartBeat should be a positive timestamp";
  }

  @Test
  public void testSafelySetUnhealthyNode_singleNode() throws Exception {
    var serviceRegistry = testResourceFactory.serviceRegistry(testNode);
    serviceRegistry.registerMetricProcessor();
    serviceRegistry.safelySetUnhealthyNodes(Arrays.asList(testNode.id()));
    var unhealthyNodes = serviceRegistry.getUnhealthyNodes();
    assert unhealthyNodes.contains(testNode.id()) : "Unhealthy node should be recorded";
  }

  @Test
  public void testSafelySetUnhealthyNode_emptyList() throws Exception {
    var serviceRegistry = testResourceFactory.serviceRegistry(testNode);
    serviceRegistry.registerMetricProcessor();
    serviceRegistry.safelySetUnhealthyNodes(Arrays.asList());
    var unhealthyNodes = serviceRegistry.getUnhealthyNodes();
    assert unhealthyNodes.isEmpty() : "Unhealthy nodes should be empty when no nodes are set";
  }

  // test with multiple nodes
  @Test
  public void testSafelySetUnhealthyNode_multipleNodes() throws Exception {
    var serviceRegistry = testResourceFactory.serviceRegistry(testNode);
    serviceRegistry.registerMetricProcessor();
    var node2 = testResourceFactory.makeNode("test-node-2");
    serviceRegistry.safelySetUnhealthyNodes(Arrays.asList(testNode.id(), node2.id()));
    var unhealthyNodes = serviceRegistry.getUnhealthyNodes();
    assert unhealthyNodes.contains(testNode.id()) : "Unhealthy node 1 should be recorded";
    assert unhealthyNodes.contains(node2.id()) : "Unhealthy node 2 should be recorded";
  }

  @Test
  public void testSafeUpdateNodes_noLeader() throws Exception {
    var serviceRegistry = testResourceFactory.serviceRegistry(testNode);
    serviceRegistry.registerMetricProcessor();
    assertThrows(
        IllegalStateException.class,
        () -> serviceRegistry.safelyUpdateNodes(Arrays.asList("node-1", "node-2")));
  }

  @Test
  public void testSafeUpdateNodes_noPrevious() throws Exception {
    var serviceRegistry = testResourceFactory.serviceRegistry(testNode);
    serviceRegistry.registerMetricProcessor();
    testResourceFactory.zkResources(testNode).setLeader(true);
    serviceRegistry.safelyUpdateNodes(Arrays.asList(testNode.id(), "test-node-2"));
    var configUpdateOp = serviceRegistry.clusterChangeOp().get();
    assertEquals(TWO_PHASE_STATE.START, configUpdateOp.state());
    var oldConfig = serviceRegistry.oldClusterConfig().get();
    var newConfig = serviceRegistry.latestClusterConfig();
    assertEquals(Arrays.asList(testNode.id()), oldConfig.nodes());
    assertEquals(Arrays.asList(testNode.id(), "test-node-2"), newConfig.nodes());

    assertThrows(
        IllegalStateException.class,
        () ->
            serviceRegistry.safelyUpdateNodes(
                Arrays.asList(testNode.id(), "test-node-2", "test-node-3")));
    // close the previous operation
    var clusterChangeOp = serviceRegistry.clusterChangeOp().get();
    serviceRegistry.safelyUpdateClusterOpState(clusterChangeOp.opId(), TWO_PHASE_STATE.DONE);
    // start a new operation, to check that state is updated
    serviceRegistry.safelyUpdateNodes(Arrays.asList(testNode.id(), "test-node-2", "test-node-3"));
    clusterChangeOp = serviceRegistry.clusterChangeOp().get();
    assertEquals(TWO_PHASE_STATE.START, clusterChangeOp.state());

    newConfig = serviceRegistry.latestClusterConfig();
    oldConfig = serviceRegistry.oldClusterConfig().get();
    assertEquals(Arrays.asList(testNode.id(), "test-node-2"), oldConfig.nodes());
    assertEquals(Arrays.asList(testNode.id(), "test-node-2", "test-node-3"), newConfig.nodes());
  }

  @Test
  public void testClusterOpStateUpdate_nomatchingId() throws Exception {
    var serviceRegistry = testResourceFactory.serviceRegistry(testNode);
    serviceRegistry.registerMetricProcessor();
    testResourceFactory.zkResources(testNode).setLeader(true);
    serviceRegistry.safelyUpdateNodes(Arrays.asList(testNode.id(), "test-node-1"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            serviceRegistry.safelyUpdateClusterOpState(
                UUID.randomUUID().toString(), TWO_PHASE_STATE.DONE));
  }

  @Test
  public void testListActive() throws Exception {
    var serviceRegistry = testResourceFactory.serviceRegistry(testNode);
    serviceRegistry.registerMetricProcessor();
    testResourceFactory.zkResources(testNode).setLeader(true);
    serviceRegistry.safelyUpdateNodes(Arrays.asList(testNode.id()));
    var opId = serviceRegistry.clusterChangeOp().get().opId();
    serviceRegistry.safelyUpdateClusterOpState(opId, TWO_PHASE_STATE.DONE);
    var active = serviceRegistry.listActiveNodes();
    assertEquals(1, active.get().size());

    // without a 2pc only node-1 is marked as active.
    var testNode2 = new Node("test-node-2", "local-2", NodeState.NODE_CREATED);
    testResourceFactory.serviceRegistry(testNode2).registerMetricProcessor();
    var registered = serviceRegistry.listRegisteredMetricNodes();
    assertEquals(Set.of("test-node", "test-node-2"), registered);
    assertEquals(1, serviceRegistry.listActiveNodes().get().size());

    // update the cluster to also make node-2 active.
    serviceRegistry.safelyUpdateNodes(Arrays.asList(testNode2.id(), testNode.id()));
    assertTrue(serviceRegistry.listActiveNodes().isEmpty());
    var clusterOp = serviceRegistry.clusterChangeOp();
    serviceRegistry.safelyUpdateClusterOpState(clusterOp.get().opId(), TWO_PHASE_STATE.DONE);
    assertEquals(2, serviceRegistry.listActiveNodes().get().size());
    assertEquals(
        Set.of("test-node-2", "test-node"), Set.copyOf(serviceRegistry.listActiveNodes().get()));
  }
}
