package org.okapi.web.auth.tx;

import static org.okapi.data.ddb.attributes.ENTITY_TYPE.ORG;
import static org.okapi.data.ddb.attributes.ENTITY_TYPE.USER;

import lombok.AllArgsConstructor;
import org.okapi.data.ddb.attributes.EntityId;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.ddb.attributes.RELATION_TYPE;
import org.okapi.web.auth.GraphTx;

@AllArgsConstructor
public class MakeUserOrgAdmin implements GraphTx {
  String userId;
  String orgId;

  @Override
  public void doTx(RelationGraphDao relationGraphDao) {
    relationGraphDao.addRelationship(
        EntityId.of(USER, userId), EntityId.of(ORG, orgId), RELATION_TYPE.ORG_ADMIN);
    relationGraphDao.addRelationship(
        EntityId.of(ORG, orgId), EntityId.of(USER, userId), RELATION_TYPE.ORG_ADMIN);
    relationGraphDao.addRelationship(
        EntityId.of(ORG, orgId), EntityId.of(USER, userId), RELATION_TYPE.ORG_MEMBER);
  }
}
