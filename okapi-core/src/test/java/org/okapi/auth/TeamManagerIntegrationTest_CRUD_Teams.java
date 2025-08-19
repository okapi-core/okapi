package org.okapi.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.okapi.rest.org.UpdateTeamRequest;
import com.okapi.rest.team.CreateTeamRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.fixtures.Deduplicator;
import org.okapi.fixtures.SingletonFactory;
import org.okapi.metrics.IdCreationFailedException;

public class TeamManagerIntegrationTest_CRUD_Teams {

  static Class<TeamManagerIntegrationTest_CRUD_Teams> thisClass =
      TeamManagerIntegrationTest_CRUD_Teams.class;
  public static final String ORG_ID = Deduplicator.dedup("org_org-12345", thisClass);
  public static final String ADMIN_USER_ID = "admin-user-12345";
  public static final String NON_ADMIN_USER_ID = "non-admin-user-12345";
  public static final String TEAM_NAME = "Test Team";

  SingletonFactory singletonFactory;
  TokenManager tokenManager;
  TeamsManager teamManager;

  String adminLoginToken;
  String adminAccessToken;

  String nonAdminLoginToken;
  String nonAdminAccessToken;

  @BeforeEach
  public void setup() throws UnAuthorizedException {
    singletonFactory = new SingletonFactory();
    tokenManager = singletonFactory.tokenManager();
    teamManager = singletonFactory.teamsManager();
    adminLoginToken = tokenManager.issueLoginToken(ADMIN_USER_ID);
    var usersDao = singletonFactory.usersDao();
    usersDao.grantRole(ADMIN_USER_ID, RoleTemplates.getOrgAdminRole(ORG_ID));
    usersDao.grantRole(ADMIN_USER_ID, RoleTemplates.getOrgMemberRole(ORG_ID));
    adminAccessToken = tokenManager.issueTemporaryBearerToken(ORG_ID, adminLoginToken);
    nonAdminLoginToken = tokenManager.issueLoginToken(NON_ADMIN_USER_ID);
    usersDao.grantRole(NON_ADMIN_USER_ID, RoleTemplates.getOrgMemberRole(ORG_ID));
    nonAdminAccessToken = tokenManager.issueTemporaryBearerToken(ORG_ID, nonAdminLoginToken);
  }

  @Test
  public void testAdminUserCanCreateTeam()
      throws UnAuthorizedException, IdCreationFailedException, BadRequestException {
    // This test should create a team and verify its creation
    // Implementation goes here
    var createTeamRequest = new CreateTeamRequest(TEAM_NAME, "A test team");
    var echo = teamManager.createTeam(adminAccessToken, createTeamRequest);
    var listedTeams = teamManager.listTeams(adminAccessToken);
    var matchingTeam =
        listedTeams.getTeams().stream()
            .filter(t -> t.getTeamId().equals(echo.getTeamId()))
            .toList();
    assertEquals(1, matchingTeam.size(), "Team should be created and listed");
    assertEquals(TEAM_NAME, matchingTeam.get(0).getTeamName(), "Team name should match");
    assertEquals(
        "A test team", matchingTeam.get(0).getDescription(), "Team description should match");
  }

  @Test
  public void testNonAdminUserCannotCreateTeam() throws IdCreationFailedException {
    // This test should verify that a non-admin user cannot create a team
    var createTeamRequest = new CreateTeamRequest(TEAM_NAME, "A test team");
    try {
      teamManager.createTeam(nonAdminAccessToken, createTeamRequest);
    } catch (UnAuthorizedException e) {
      // Expected exception
      return;
    }
    throw new AssertionError("Expected UnAuthorizedException was not thrown");
  }

  @Test
  public void testAdminUserCanUpdateTeam()
      throws UnAuthorizedException, BadRequestException, IdCreationFailedException {
    // This test should create a team and then update it
    var createTeamRequest = new CreateTeamRequest(TEAM_NAME, "A test team");
    var createdTeam = teamManager.createTeam(adminAccessToken, createTeamRequest);

    // Now update the team
    var updateRequest = new UpdateTeamRequest("Updated Team", "Updated description");
    teamManager.updateTeam(adminAccessToken, createdTeam.getTeamId(), updateRequest);

    // Verify the update
    var updatedTeams = teamManager.listTeams(adminAccessToken);
    var matchingTeam =
        updatedTeams.getTeams().stream()
            .filter(t -> t.getTeamId().equals(createdTeam.getTeamId()))
            .toList();
    assertEquals(1, matchingTeam.size(), "Updated team should be listed");
    assertEquals(
        "Updated Team", matchingTeam.get(0).getTeamName(), "Updated team name should match");
    assertEquals(
        "Updated description",
        matchingTeam.get(0).getDescription(),
        "Updated description should match");
  }
}
