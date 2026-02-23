/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.ddb;

import static org.junit.jupiter.api.Assertions.*;

import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.data.CreateDynamoDBTables;
import org.okapi.data.dao.InfraEntityNodeDao;
import org.okapi.data.ddb.attributes.DEP_TYPE;
import org.okapi.data.ddb.attributes.INFRA_ENTITY_TYPE;
import org.okapi.data.ddb.attributes.InfraEntityId;
import org.okapi.data.ddb.attributes.InfraNodeOutgoingEdge;
import org.okapi.data.dto.InfraEntityNodeDdb;
import org.okapi.data.exceptions.EntityDoesNotExistException;
import org.okapi.testutils.OkapiTestUtils;

@Execution(ExecutionMode.CONCURRENT)
public class InfraEntityNodeDaoDdbImplIT {

  InfraEntityNodeDao dao;

  String tenant;
  InfraEntityId node1;
  InfraEntityId node2;
  InfraEntityId node3;
  Injector injector;

  @BeforeEach
  public void setup() {
    CreateDynamoDBTables.createTables(OkapiTestUtils.getLocalStackDynamoDbClient());
    injector = Injectors.createTestInjector();
    dao = injector.getInstance(InfraEntityNodeDao.class);
    tenant = OkapiTestUtils.getTestId(InfraEntityNodeDaoDdbImplIT.class);
    node1 = new InfraEntityId(tenant, INFRA_ENTITY_TYPE.SERVICE, "n1");
    node2 = new InfraEntityId(tenant, INFRA_ENTITY_TYPE.SERVICE, "n2");
    node3 = new InfraEntityId(tenant, INFRA_ENTITY_TYPE.HOST, "n3");
  }

  @Test
  public void fullLifecycle_nodes_and_edges() throws EntityDoesNotExistException {
    // create nodes
    dao.createNode(new InfraEntityNodeDdb(node1, "attrs-n1", null));
    dao.createNode(new InfraEntityNodeDdb(node2, "attrs-n2", null));
    dao.createNode(new InfraEntityNodeDdb(node3, "attrs-n3", null));

    // add two outgoing edges from node1 -> node2, node3
    dao.addOutgoingEdge(node1, new InfraNodeOutgoingEdge(node2, "e12", DEP_TYPE.CONSUMES));
    dao.addOutgoingEdge(node1, new InfraNodeOutgoingEdge(node3, "e13", DEP_TYPE.RUNS_ON));

    // query edges by type
    var consumes = dao.getEdgesByType(node1, DEP_TYPE.CONSUMES);
    assertEquals(1, consumes.size());
    assertEquals(node2, consumes.get(0).getTargetNodeId());
    var runsOn = dao.getEdgesByType(node1, DEP_TYPE.RUNS_ON);
    assertEquals(1, runsOn.size());
    assertEquals(node3, runsOn.get(0).getTargetNodeId());
    var monitored = dao.getEdgesByType(node1, DEP_TYPE.MONITORED_BY);
    assertEquals(0, monitored.size());

    // get all edges
    var all = dao.getAllOutgoingEdges(node1);
    assertEquals(2, all.size());

    // get the node itself via DAO
    var fetchedOpt = dao.getNode(node1);
    assertTrue(fetchedOpt.isPresent());
    assertEquals("attrs-n1", fetchedOpt.get().getAttributes());
  }

  @Test
  public void create_edge_to_nonexistent_target_should_throw_checked_exception() {
    dao.createNode(new InfraEntityNodeDdb(node1, "attrs-n1", null));
    var ghost = new InfraEntityId(tenant, INFRA_ENTITY_TYPE.SERVICE, "ghost");
    assertThrows(
        org.okapi.data.exceptions.EntityDoesNotExistException.class,
        () ->
            dao.addOutgoingEdge(node1, new InfraNodeOutgoingEdge(ghost, "e1g", DEP_TYPE.CONSUMES)));
  }

  // no direct table access helpers required
}
