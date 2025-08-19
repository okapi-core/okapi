package org.okapi.data.dao;

public interface DaoFactory {
  AuthorizationTokenDao authorizationTokenDao();

  DashboardDao dashboardDao();

  OrgDao orgDao();

  RelationGraphDao relationGraphDao();

  TeamMemberDao teamMemberDao();

  TeamsDao teamsDao();

  UsersDao usersDao();
}
