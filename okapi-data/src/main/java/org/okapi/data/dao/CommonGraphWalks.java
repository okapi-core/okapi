package org.okapi.data.dao;

import static org.okapi.data.dao.RelationGraphDao.makeRelation;
import static org.okapi.data.dto.RelationGraphNode.ENTITY_TYPE.*;
import static org.okapi.data.dto.RelationGraphNode.RELATION_TYPE.ORG_MEMBER;
import static org.okapi.data.dto.RelationGraphNode.RELATION_TYPE.TEAM_MEMBER;
import static org.okapi.data.dto.RelationGraphNode.makeEntityId;

import java.util.Arrays;
import java.util.Collections;

public class CommonGraphWalks {

  public RelationGraphDao relationGraphDao;

  public boolean userIsOrgMember(String userId, String orgId) {
    var userEntity = makeEntityId(USER, userId);
    var orgEntity = makeEntityId(ORG, orgId);
    var path = new RelationGraphDao.EdgeSeq(Arrays.asList(makeRelation(ORG, ORG_MEMBER)));
    return relationGraphDao.aPathExists(userEntity, orgEntity, Collections.singletonList(path));
  }

  public boolean userIsTeamMember(String userId, String teamId) {
    var userEntity = makeEntityId(USER, userId);
    var orgEntity = makeEntityId(TEAM, teamId);
    var path = new RelationGraphDao.EdgeSeq(Arrays.asList(makeRelation(TEAM, TEAM_MEMBER)));
    return relationGraphDao.aPathExists(userEntity, orgEntity, Collections.singletonList(path));
  }
}
