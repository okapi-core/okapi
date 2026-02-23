/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.auth;

import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.web.auth.tx.AddMemberToOrgTx;
import org.okapi.web.auth.tx.MakeUserOrgAdmin;

public class TestCommons {
  public static void addToOrg(
      UsersDao usersDao, RelationGraphDao graphDao, String orgId, String email, boolean isAdmin) {
    var user = usersDao.getWithEmail(email).get();
    var addUserTx = new AddMemberToOrgTx(user.getUserId(), orgId);
    addUserTx.doTx(graphDao);
    if (isAdmin) {
      var makeUserAdminTx = new MakeUserOrgAdmin(user.getUserId(), orgId);
      makeUserAdminTx.doTx(graphDao);
    }
  }
}
