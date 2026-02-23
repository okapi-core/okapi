/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.auth.tx;

import static org.okapi.data.ddb.attributes.ENTITY_TYPE.DASHBOARD;
import static org.okapi.data.ddb.attributes.ENTITY_TYPE.ORG;

import lombok.AllArgsConstructor;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.ddb.attributes.EntityId;
import org.okapi.data.ddb.attributes.RELATION_TYPE;
import org.okapi.web.auth.GraphTx;

@AllArgsConstructor
public class GrantOrgReadToDashboardTx implements GraphTx {
  String orgId;
  String dashboardId;

  @Override
  public void doTx(RelationGraphDao relationGraphDao) {
    relationGraphDao.addRelationship(
        EntityId.of(ORG, orgId), EntityId.of(DASHBOARD, dashboardId), RELATION_TYPE.DASHBOARD_READ);
  }
}
