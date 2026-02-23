package org.okapi.web.auth.tx;

import static org.okapi.data.ddb.attributes.ENTITY_TYPE.ORG;

import lombok.AllArgsConstructor;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.ddb.attributes.EntityId;
import org.okapi.data.ddb.attributes.ENTITY_TYPE;
import org.okapi.data.ddb.attributes.RELATION_TYPE;
import org.okapi.web.auth.GraphTx;

@AllArgsConstructor
public class AddMemberToOrgTx implements GraphTx {
  String userId;
  String orgId;

  @Override
  public void doTx(RelationGraphDao relationGraphDao) {
    relationGraphDao.addRelationship(
        EntityId.of(ENTITY_TYPE.USER, userId), EntityId.of(ORG, orgId), RELATION_TYPE.ORG_MEMBER);

    // inverse relation
    relationGraphDao.addRelationship(
        EntityId.of(ORG, orgId), EntityId.of(ENTITY_TYPE.USER, userId), RELATION_TYPE.ORG_MEMBER);
  }
}
