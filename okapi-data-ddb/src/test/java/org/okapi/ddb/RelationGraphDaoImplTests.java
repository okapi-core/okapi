/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.ddb;

import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.data.dao.RelationGraphDao.makeRelation;
import static org.okapi.data.ddb.attributes.ENTITY_TYPE.*;
import static org.okapi.data.ddb.attributes.RELATION_TYPE.*;
import static org.okapi.data.dto.RelationGraphNodeDdb.makeEntityId;
import static org.okapi.fixtures.Deduplicator.dedup;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.ddb.attributes.EdgeSeq;
import org.okapi.data.ddb.attributes.EntityId;
import org.okapi.data.ddb.dao.RelationGraphDaoImpl;
import org.okapi.data.ddb.dao.RelationGraphNode;
import org.okapi.data.migrations.RelationGraphDdbSpec;
import org.okapi.testutils.OkapiTestUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class RelationGraphDaoImplTests {

  RelationGraphDao dao;
  DynamoDbClient ddbClient = OkapiTestUtils.getLocalStackDynamoDbClient();
  DynamoDbEnhancedClient enhancedClient =
      DynamoDbEnhancedClient.builder().dynamoDbClient(ddbClient).build();

  String testId = OkapiTestUtils.getTestId(RelationGraphDaoImplTests.class);
  String userA = dedup(testId, "userA");
  String orgA = dedup(testId, "orgA");
  String dashRead = dedup(testId, "dashA");
  String dashEdit = dedup(testId, "dashB");
  String userB = dedup(testId, "userB");
  String orgB = dedup(testId, "orgB");
  String dashB = dedup(testId, "dashB");

  String globalUser = dedup(testId, "globalUser");

  // list of accepted paths for dash edit and read
  List<EdgeSeq> dashEditRoute =
      List.of(
          new EdgeSeq(
              Arrays.asList(
                  makeRelation(ORG, ORG_MEMBER), makeRelation(DASHBOARD, DASHBOARD_EDIT))));

  List<EdgeSeq> dashReadRoute =
      List.of(
          new EdgeSeq(
              Arrays.asList(
                  makeRelation(ORG, ORG_MEMBER), makeRelation(DASHBOARD, DASHBOARD_READ))));

  List<EdgeSeq> orgMemberRoute = List.of(new EdgeSeq(Arrays.asList(makeRelation(ORG, ORG_MEMBER))));

  @BeforeEach
  public void setup() {
    var spec = new RelationGraphDdbSpec();
    spec.create(ddbClient);
    dao = new RelationGraphDaoImpl(enhancedClient);
    // A is realted to orgA
    dao.addRelationship(new EntityId(USER, userA), makeEntityId(ORG, orgA), ORG_MEMBER);

    // B is related to orgB
    dao.addRelationship(makeEntityId(USER, userB), makeEntityId(ORG, orgB), ORG_MEMBER);

    // anyone in orgA has read access to dashA and edit access to dashB
    dao.addRelationship(makeEntityId(ORG, orgA), makeEntityId(DASHBOARD, dashRead), DASHBOARD_READ);
    dao.addRelationship(makeEntityId(ORG, orgA), makeEntityId(DASHBOARD, dashEdit), DASHBOARD_EDIT);

    // dashB is only accessible by orgB members with read access
    dao.addRelationship(makeEntityId(ORG, orgB), makeEntityId(DASHBOARD, dashB), DASHBOARD_READ);

    // globalUser is member of orgA and orgB
    dao.addRelationship(new EntityId(USER, globalUser), makeEntityId(ORG, orgA), ORG_MEMBER);
    dao.addRelationship(new EntityId(USER, globalUser), makeEntityId(ORG, orgB), ORG_MEMBER);
  }

  @Test
  public void testRelations() {
    // single paths
    assertTrue(
        dao.isAnyPathBetween(makeEntityId(USER, userA), makeEntityId(ORG, orgA), orgMemberRoute));

    // any path
    assertTrue(
        dao.isAnyPathBetween(
            makeEntityId(USER, globalUser), makeEntityId(DASHBOARD, dashRead), dashReadRoute));

    assertTrue(
        dao.isAnyPathBetween(
            makeEntityId(USER, globalUser), makeEntityId(DASHBOARD, dashEdit), dashEditRoute));

    // global user can also access dashB with read access
    assertTrue(
        dao.isAnyPathBetween(
            makeEntityId(USER, globalUser), makeEntityId(DASHBOARD, dashB), dashReadRoute));

    // negative tests
    assertFalse(
        dao.isAnyPathBetween(
            makeEntityId(USER, userB), makeEntityId(DASHBOARD, dashRead), dashReadRoute));
    assertTrue(
        dao.hasRelationBetween(makeEntityId(USER, userA), makeEntityId(ORG, orgA), ORG_MEMBER));
    assertFalse(
        dao.hasRelationBetween(makeEntityId(USER, userA), makeEntityId(ORG, orgB), ORG_MEMBER));

    // get all relations of type
    var relations = dao.getAllRelationsOfNodeType(EntityId.of(USER, globalUser), ORG);
    assertEquals(2, relations.size());

    var orgIds = relations.stream().map(RelationGraphNode::getRelatedEntity).toList();
    assertTrue(orgIds.contains(EntityId.of(ORG, orgA).toString()));
    assertTrue(orgIds.contains(EntityId.of(ORG, orgB).toString()));

    // delete
    dao.deleteEntity(EntityId.of(USER, userA));
    assertFalse(
        dao.isAnyPathBetween(
            makeEntityId(USER, userA), makeEntityId(DASHBOARD, dashRead), dashReadRoute));
    dao.deleteEntity(EntityId.of(ORG, orgB));
    assertFalse(
        dao.isAnyPathBetween(
            makeEntityId(USER, globalUser), makeEntityId(DASHBOARD, dashB), dashReadRoute));
  }
}
