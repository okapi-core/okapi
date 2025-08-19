package org.okapi.metrics.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;

public class LeaderJobsImplTest {

  TestResourceFactory testResourceFactory;
  Node leader;
  Node follower;

  @BeforeEach
  public void setup() {
    testResourceFactory = new TestResourceFactory();
    leader = testResourceFactory.makeNode("leader");
    follower = testResourceFactory.makeNode("follower");
  }

  @Test
  public void testHealthCheckWithAllHealthyInstances() throws Exception {
    var zkResources = testResourceFactory.zkResources(leader);
    zkResources.setLeader(true);
    var registry = testResourceFactory.serviceRegistry(leader);
    registry.registerMetricProcessor();
    registry.writeHeartBeat();

    var leader = testResourceFactory.leaderJobs(this.leader);
    leader.checkFleetHealth();
  }

  @Test
  public void testFailedCountIncreasedWithoutHeartBeat() throws Exception {
    var testNode2 = new Node("test-node-2", "localhost", NodeState.METRICS_CONSUMPTION_START);
    var node1 = testResourceFactory.serviceRegistry(leader);
    var node2 = testResourceFactory.serviceRegistry(testNode2);
    node1.registerMetricProcessor();
    node2.registerMetricProcessor();
    node1.writeHeartBeat();
    node2.writeHeartBeat();

    var leader = testResourceFactory.leaderJobs(this.leader);
    leader.checkFleetHealth();
  }

  @Test
  public void testReshardingTriggeredWithLeader() throws Exception {
    // make two nodes, set leader, fail heart beat check for both, check that resharding is
    // triggered
    var testNode2 = new Node("test-node-2", "localhost", NodeState.METRICS_CONSUMPTION_START);
    var node1 = testResourceFactory.serviceRegistry(leader);
    var node2 = testResourceFactory.serviceRegistry(testNode2);
    node1.registerMetricProcessor();
    node2.registerMetricProcessor();

    var zkResources = testResourceFactory.zkResources(leader);
    zkResources.setLeader(true);
    node1.writeHeartBeat();
    node2.writeHeartBeat();

    var leader = testResourceFactory.leaderJobs(this.leader);
    leader.checkFleetHealth();
  }
}
