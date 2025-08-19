package org.okapi.auth;

import org.okapi.data.dao.UsersDao;
import org.okapi.exceptions.UnAuthorizedException;

public class AccessManager {
  UsersDao usersDao;

  public AccessManager(UsersDao usersDao) {
    this.usersDao = usersDao;
  }

  private void checkUserHasRole(String userId, String role) throws UnAuthorizedException {
    var maybeRole = usersDao.checkRole(userId, role);
    if (maybeRole.isEmpty()) {
      throw new UnAuthorizedException();
    }
  }

  public void checkUserHasIsOrgAdmin(String userId, String org) throws UnAuthorizedException {
    checkUserHasRole(userId, RoleTemplates.getOrgAdminRole(org));
  }

  public void checkUserIsClusterAdmin(String userId, String clusterId) throws UnAuthorizedException {
    checkUserHasRole(userId, RoleTemplates.getClusterAdminRole(clusterId));
  }

  public void checkUserHasIsOrgMember(String userId, String org) throws UnAuthorizedException {
    checkUserHasRole(userId, RoleTemplates.getOrgMemberRole(org));
  }
  public void checkUserIsTeamAdmin(String userId, String orgId, String teamId)
      throws UnAuthorizedException {
    checkUserHasRole(userId, RoleTemplates.getTeamAdminRole(orgId, teamId));
  }

  public void checkUserCanWriteForTeam(String userId, String orgId, String teamId) throws UnAuthorizedException {
    checkUserHasRole(userId, RoleTemplates.getTeamWriterRole(orgId, teamId));
  }

  public void checkUserCanReadFromTeam(String userId, String orgId, String teamId) throws UnAuthorizedException {
    checkUserHasRole(userId, RoleTemplates.getTeamReaderRole(orgId, teamId));
  }
}
