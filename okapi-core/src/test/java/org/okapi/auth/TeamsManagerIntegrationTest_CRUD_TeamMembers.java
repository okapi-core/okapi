package org.okapi.auth;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.auth.TestCommons.addToOrg;

import com.google.common.collect.Lists;
import com.okapi.rest.auth.AuthRequest;
import com.okapi.rest.team.CreateTeamMemberRequest;
import com.okapi.rest.team.CreateTeamRequest;
import com.okapi.rest.team.GetTeamResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.data.dao.TeamsDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.data.dto.UserRoleRelationDto;
import org.okapi.data.exceptions.UserAlreadyExistsException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.fixtures.Deduplicator;
import org.okapi.fixtures.SingletonFactory;
import org.okapi.metrics.IdCreationFailedException;

@Execution(ExecutionMode.CONCURRENT)
public class TeamsManagerIntegrationTest_CRUD_TeamMembers {
  static Class<TeamsManagerIntegrationTest_CRUD_TeamMembers> thisClass =
      TeamsManagerIntegrationTest_CRUD_TeamMembers.class;
  public static final String ORG_ID = Deduplicator.dedup("org-12345", thisClass);
  public static final String ADMIN_USER_ID = "main-admin-user";
  public static final String TEAM_NAME = "Test Team";
  SingletonFactory singletonFactory;
  String adminLoginToken;
  String adminAccessToken;
  UsersDao usersDao;
  TeamsManager teamManager;
  TeamsDao teamsDao;
  TokenManager tokenManager;

  GetTeamResponse getTeamResponse;

  @BeforeEach
  public void setup()
      throws UnAuthorizedException, UserAlreadyExistsException, IdCreationFailedException {
    singletonFactory = new SingletonFactory();
    singletonFactory.usersDao().grantRole(ADMIN_USER_ID, RoleTemplates.getOrgAdminRole(ORG_ID));
    singletonFactory.usersDao().grantRole(ADMIN_USER_ID, RoleTemplates.getOrgMemberRole(ORG_ID));
    adminLoginToken = singletonFactory.tokenManager().issueLoginToken(ADMIN_USER_ID);
    adminAccessToken =
        singletonFactory.tokenManager().issueTemporaryBearerToken(ORG_ID, adminLoginToken);
    usersDao = singletonFactory.usersDao();
    teamsDao = singletonFactory.teamsDao();
    teamManager = singletonFactory.teamsManager();
    tokenManager = singletonFactory.tokenManager();
    setUpOtherUsers();
    createTeam();
  }

  private void setUpOtherUsers() throws UserAlreadyExistsException {
    try {
      singletonFactory.usersDao().create("admin", "2", "admin2@domain.com", "password123");
    } catch (UserAlreadyExistsException e) {
      // Ignore if user already exists
    }
    try {
      usersDao.create("", "1", "reader@domain.com", "password123");
    } catch (UserAlreadyExistsException e) {
      // Ignore if user already exists
    }
    try {
      usersDao.create("", "1", "writer@domain.com", "password123");
    } catch (UserAlreadyExistsException e) {
      // Ignore if user already exists
    }
    try {
      usersDao.create("", "non-admin", "nonTeamAdmin@domain.com", "password123");
    } catch (UserAlreadyExistsException e) {
      // Ignore if user already exists
    }
    addToOrg(singletonFactory, ORG_ID, "admin2@domain.com", true);
    for (var email :
        new String[] {"reader@domain.com", "writer@domain.com", "nonTeamAdmin@domain.com"}) {
      addToOrg(singletonFactory, ORG_ID, email, false);
    }
  }

  private void createTeam() throws UnAuthorizedException, IdCreationFailedException {
    var createTeamRequest = new CreateTeamRequest(TEAM_NAME, "A test team");
    getTeamResponse = teamManager.createTeam(adminAccessToken, createTeamRequest);
  }

  @Test
  public void testTeamAdminCanAddRemoveMembers() throws UnAuthorizedException, BadRequestException {
    // todo: debug this test, find out what's wrong.
    var teamId = getTeamResponse.getTeamId();
    var teamDto = teamsDao.get(teamId).orElseThrow(() -> new BadRequestException("Team not found"));
    var orgId = teamDto.getOrgId();
    var createAdminMemberRequest =
        new CreateTeamMemberRequest("admin2@domain.com", true, true, true);
    teamManager.addTeamMember(
        adminAccessToken, getTeamResponse.getTeamId(), createAdminMemberRequest);

    var listedMembers = teamManager.listTeamMembers(adminAccessToken, getTeamResponse.getTeamId());
    var matchingMember =
        listedMembers.getAdmins().stream()
            .filter(e -> e.equals(createAdminMemberRequest.getEmail()))
            .findFirst();
    assertTrue(matchingMember.isPresent(), "Admin member should be added to the team");
    var targetUserFromEmail = usersDao.getWithEmail(createAdminMemberRequest.getEmail());
    var userId = targetUserFromEmail.get().getUserId();
    var roles =
        Lists.newArrayList(usersDao.listRolesByUserId(userId)).stream()
            .filter(
                userRoleRelationDto ->
                    userRoleRelationDto.getStatus() == UserRoleRelationDto.STATUS.ACTIVE)
            .map(UserRoleRelationDto::getRole)
            .toList();

    assertTrue(roles.contains(RoleTemplates.getTeamAdminRole(orgId, teamId)));
    assertTrue(roles.contains(RoleTemplates.getTeamWriterRole(orgId, teamId)));
    assertTrue(roles.contains(RoleTemplates.getTeamReaderRole(orgId, teamId)));

    // delete
    teamManager.deleteTeamMember(
        adminAccessToken, getTeamResponse.getTeamId(), createAdminMemberRequest.getEmail());
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(
            () -> {
              var listedMembersAfterDelete =
                  teamManager.listTeamMembers(adminAccessToken, getTeamResponse.getTeamId());
              var matchingMemberAfterDelete =
                  listedMembersAfterDelete.getAdmins().stream()
                      .filter(e -> e.equals(createAdminMemberRequest.getEmail()))
                      .findFirst();
              return matchingMemberAfterDelete.isEmpty();
            });
  }

  @Test
  public void testNonAdminTeamMemberCannotAddMembers()
      throws BadRequestException, UnAuthorizedException {
    // This test should verify that a non-admin user cannot add members to a team
    var teamId = getTeamResponse.getTeamId();
    var createAdminMemberRequest =
        new CreateTeamMemberRequest("nonTeamAdmin@domain.com", true, true, false);
    teamManager.addTeamMember(adminAccessToken, teamId, createAdminMemberRequest);

    // sign in non-admin user
    var nonAdminUser =
        usersDao
            .getWithEmail("nonTeamAdmin@domain.com")
            .orElseThrow(() -> new BadRequestException("User not found"));
    usersDao.grantRole(nonAdminUser.getUserId(), RoleTemplates.getOrgMemberRole(ORG_ID));
    var nonAdminLoginToken = tokenManager.issueLoginToken(nonAdminUser.getUserId());
    var nonAdminAccessToken = tokenManager.issueTemporaryBearerToken(ORG_ID, nonAdminLoginToken);
    try {
      var createNonAdminMemberRequest =
          new CreateTeamMemberRequest("doesntMatter@domain.com", true, true, true);
      teamManager.addTeamMember(
          nonAdminAccessToken, getTeamResponse.getTeamId(), createNonAdminMemberRequest);
      assertTrue(false, "Non-admin user should not be able to add members");
    } catch (UnAuthorizedException e) {
      assertTrue(true, "Non-admin user should not be able to add members");
    }
    // Expected exception, as non-admin user should not be able to add members
  }

  @Test
  public void teamTeamShowsUpInListOfTeams() throws UnAuthorizedException, BadRequestException {
    // This test should verify that the team created shows up in the list of teams
    var teamId = getTeamResponse.getTeamId();
    var teamDto = teamsDao.get(teamId).orElseThrow(() -> new BadRequestException("Team not found"));
    teamManager.addTeamMember(
        adminAccessToken,
        getTeamResponse.getTeamId(),
        new CreateTeamMemberRequest("admin2@domain.com", true, true, true));
    // admin member
    var admin2LoginToken =
        singletonFactory
            .getUserManager()
            .signInWithEmailPassword(
                new AuthRequest("Oscar", "Okapi", "admin2@domain.com", "password123"));
    var admin2Bearer =
        singletonFactory.tokenManager().issueTemporaryBearerToken(ORG_ID, admin2LoginToken);
    var listedTeams = teamManager.listTeams(admin2Bearer);
    var matchingTeam =
        listedTeams.getTeams().stream()
            .filter(t -> t.getTeamId().equals(teamDto.getTeamId()))
            .toList();
    assertEquals(1, matchingTeam.size(), "Team should be listed for admin2 user");
    assertTrue(matchingTeam.get(0).isAdmin());

    // reader member
    teamManager.addTeamMember(
        adminAccessToken,
        getTeamResponse.getTeamId(),
        new CreateTeamMemberRequest("reader@domain.com", true, false, false));
    var readerLoginToken =
        singletonFactory
            .getUserManager()
            .signInWithEmailPassword(
                new AuthRequest("Oscar", "Okapi", "reader@domain.com", "password123"));
    var readerBearer =
        singletonFactory.tokenManager().issueTemporaryBearerToken(ORG_ID, readerLoginToken);
    var listedTeamsForReader = teamManager.listTeams(readerBearer);
    var matchingTeamForReader =
        listedTeamsForReader.getTeams().stream()
            .filter(t -> t.getTeamId().equals(teamDto.getTeamId()))
            .toList();
    assertEquals(1, matchingTeamForReader.size(), "Team should be listed for reader user");
    assertTrue(matchingTeamForReader.get(0).isReader(), "Reader should be able to see the team");
    assertFalse(matchingTeamForReader.get(0).isAdmin());
    assertFalse(matchingTeamForReader.get(0).isWriter());

    // writer member
    teamManager.addTeamMember(
        adminAccessToken,
        getTeamResponse.getTeamId(),
        new CreateTeamMemberRequest("writer@domain.com", false, true, false));
    var writerLoginToken =
        singletonFactory
            .getUserManager()
            .signInWithEmailPassword(
                new AuthRequest("Oscar", "Okapi", "writer@domain.com", "password123"));
    var writerBearer =
        singletonFactory.tokenManager().issueTemporaryBearerToken(ORG_ID, writerLoginToken);
    var listedTeamsForWriter = teamManager.listTeams(writerBearer);
    var matchingTeamForWriter =
        listedTeamsForWriter.getTeams().stream()
            .filter(t -> t.getTeamId().equals(teamDto.getTeamId()))
            .toList();
    assertEquals(1, matchingTeamForReader.size(), "Team should be listed for writer user");
    assertTrue(matchingTeamForWriter.get(0).isWriter(), "Writer should be able to see the team");
    assertFalse(matchingTeamForWriter.get(0).isAdmin());
    assertFalse(matchingTeamForWriter.get(0).isReader());
  }
}
