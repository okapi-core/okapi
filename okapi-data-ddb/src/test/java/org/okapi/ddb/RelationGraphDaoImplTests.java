package org.okapi.ddb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.okapi.data.dao.RelationGraphDao.makeRelation;
import static org.okapi.data.dto.RelationGraphNode.ENTITY_TYPE.*;
import static org.okapi.data.dto.RelationGraphNode.RELATION_TYPE.*;
import static org.okapi.data.dto.RelationGraphNodeDdb.makeEntityId;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.dao.RelationGraphDao.EdgeSeq;
import org.okapi.data.factory.ResourceFactory;

public class RelationGraphDaoImplTests {

  ResourceFactory resourceFactory;

  @BeforeEach
  public void setup() {
    resourceFactory = new ResourceFactory();
    var dao = resourceFactory.relationGraphDao();
    dao.add(makeEntityId(USER, "userA1"), makeEntityId(ORG, "orgA"), ORG_MEMBER);
    dao.add(makeEntityId(USER, "userA2"), makeEntityId(ORG, "orgA2"), ORG_MEMBER);
    dao.add(makeEntityId(USER, "userA1"), makeEntityId(TEAM, "teamA"), TEAM_ADMIN);
    dao.add(makeEntityId(USER, "userA2"), makeEntityId(TEAM, "teamA"), TEAM_MEMBER);
    dao.add(makeEntityId(USER, "userB1"), makeEntityId(ORG, "orgB"), ORG_MEMBER);
    dao.add(makeEntityId(ORG, "orgA"), makeEntityId(DASHBOARD, "dash_org_A"), DASHBOARD_READ);
    dao.add(makeEntityId(TEAM, "teamA"), makeEntityId(DASHBOARD, "dash_team_A"), DASHBOARD_READ);
    dao.add(makeEntityId(TEAM, "teamA"), makeEntityId(DASHBOARD, "dash_team_A"), DASHBOARD_EDIT);
  }

  @Test
  public void testSinglePath() {
    var dao = resourceFactory.relationGraphDao();
    assertTrue(
        dao.aPathExists(
            makeEntityId(USER, "userA1"),
            makeEntityId(ORG, "orgA"),
            Arrays.asList(
                new EdgeSeq(Arrays.asList(new RelationGraphDao.RelationType(ORG, ORG_MEMBER))))));
  }

  @Test
  public void testNoPath() {
    var dao = resourceFactory.relationGraphDao();
    assertFalse(
        dao.aPathExists(
            makeEntityId(USER, "userA1"),
            makeEntityId(ORG, "orgB"),
            Arrays.asList(
                new RelationGraphDao.EdgeSeq(
                    Arrays.asList(new RelationGraphDao.RelationType(ORG, ORG_MEMBER))))));
  }

  @Test
  public void testMultiplePaths_available() {
    var dao = resourceFactory.relationGraphDao();
    var paths =
        Arrays.asList(
            new EdgeSeq(
                Arrays.asList(
                    makeRelation(ORG, ORG_MEMBER), makeRelation(DASHBOARD, DASHBOARD_READ))));
    assertTrue(
        dao.aPathExists(
            makeEntityId(USER, "userA1"), makeEntityId(DASHBOARD, "dash_org_A"), paths));
  }

  @Test
  public void testMultiplePaths_not_available() {
    var dao = resourceFactory.relationGraphDao();
    var paths =
        Arrays.asList(
            new EdgeSeq(
                Arrays.asList(
                    makeRelation(ORG, ORG_MEMBER), makeRelation(DASHBOARD, DASHBOARD_READ))));
    assertFalse(
        dao.aPathExists(
            makeEntityId(USER, "userB1"), makeEntityId(DASHBOARD, "dash_org_A"), paths));
  }
}
