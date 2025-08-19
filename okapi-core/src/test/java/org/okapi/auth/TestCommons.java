package org.okapi.auth;

import org.okapi.fixtures.SingletonFactory;

public class TestCommons {
  public static void addToOrg(
          SingletonFactory singletonFactory, String orgId, String email, boolean isAdmin) {
    var userId = singletonFactory.usersDao().getWithEmail(email).get().getUserId();
    singletonFactory.usersDao().grantRole(userId, RoleTemplates.getOrgMemberRole(orgId));
    if (isAdmin) {
      singletonFactory.usersDao().grantRole(userId, RoleTemplates.getOrgAdminRole(orgId));
    }
  }
}
