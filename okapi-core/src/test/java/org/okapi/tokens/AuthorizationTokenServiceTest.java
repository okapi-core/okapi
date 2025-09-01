package org.okapi.tokens;

import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.fixtures.Deduplicator.dedup;

import com.google.common.collect.Lists;
import org.okapi.rest.tokens.CreateApiTokenRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.auth.RoleTemplates;
import org.okapi.auth.TestCommons;
import org.okapi.auth.TokenManager;
import org.okapi.data.dao.AuthorizationTokenDao;
import org.okapi.data.dao.TeamsDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.data.dto.TeamDto;
import org.okapi.data.dto.UserDto;
import org.okapi.data.exceptions.UserAlreadyExistsException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.fixtures.Deduplicator;
import org.okapi.fixtures.SingletonFactory;

@Slf4j
public class AuthorizationTokenServiceTest {

  public static final String ORG_ID =
      Deduplicator.dedup("org_org-12345", AuthorizationTokenServiceTest.class);
  AuthorizationTokenService authorizationTokenService;
  AuthorizationTokenDao authorizationTokenDao;
  SingletonFactory singletonFactory;
  TokenManager tokenManager;

  UsersDao usersDao;
  TeamsDao teamsDao;

  String orgId;
  TeamDto team;
  UserDto teamAdmin;
  UserDto notTeamAdmin;

  @BeforeEach
  public void setup() {
    singletonFactory = new SingletonFactory();
    authorizationTokenService = singletonFactory.authorizationTokenService();
    authorizationTokenDao = singletonFactory.authorizationTokenDao();
    tokenManager = singletonFactory.tokenManager();
    usersDao = singletonFactory.usersDao();
    teamsDao = singletonFactory.teamsDao();
    setupFixtures();
  }

  public void setupFixtures() {
    orgId = dedup("test-org-id", this.getClass());
    team =
        TeamDto.builder()
            .teamId(dedup("test-team-id", this.getClass()))
            .teamName("Test Team")
            .description("A test team description")
            .orgId(orgId)
            .build();

    teamAdmin =
        UserDto.builder()
            .userId(dedup("team-admin-user", this.getClass()))
            .email("admin@domain.com")
            .firstName("Team")
            .lastName("Admin")
            .build();

    notTeamAdmin =
        UserDto.builder()
            .userId(dedup("not-team-admin-user", this.getClass()))
            .email("notadmin@domain.com")
            .firstName("Team")
            .lastName("Admin")
            .build();

    teamsDao.create(team);
    try {
      teamAdmin =
          usersDao.create(
              teamAdmin.getFirstName(),
              teamAdmin.getLastName(),
              teamAdmin.getEmail(),
              teamAdmin.getHashedPassword());
    } catch (UserAlreadyExistsException e) {
      teamAdmin = usersDao.getWithEmail(teamAdmin.getEmail()).get();
    }
    try {
      notTeamAdmin =
          usersDao.create(
              notTeamAdmin.getFirstName(),
              notTeamAdmin.getLastName(),
              notTeamAdmin.getEmail(),
              notTeamAdmin.getHashedPassword());
    } catch (UserAlreadyExistsException e) {
      log.error("Got exception when creating user but moving on", e);
      notTeamAdmin = usersDao.getWithEmail(notTeamAdmin.getEmail()).get();
    }
    usersDao.grantRole(
        teamAdmin.getUserId(), RoleTemplates.getTeamAdminRole(team.getOrgId(), team.getTeamId()));
    usersDao.grantRole(
        teamAdmin.getUserId(), RoleTemplates.getTeamWriterRole(team.getOrgId(), team.getTeamId()));
    usersDao.grantRole(
        teamAdmin.getUserId(), RoleTemplates.getTeamReaderRole(team.getOrgId(), team.getTeamId()));
    TestCommons.addToOrg(singletonFactory, ORG_ID, teamAdmin.getEmail(), true);
    TestCommons.addToOrg(singletonFactory, ORG_ID, notTeamAdmin.getEmail(), false);
  }

  @Test
  public void testCreateApiTokenAuthorizationToken() throws UnAuthorizedException {
    // need to create a team with team admin relationship
    var loginToken = tokenManager.issueLoginToken(teamAdmin.getUserId());
    var tempToken = tokenManager.issueTemporaryBearerToken(ORG_ID, loginToken);
    var request = new CreateApiTokenRequest(true, true);
    var response = authorizationTokenService.createApiToken(tempToken, team.getTeamId(), request);
    var tokens = Lists.newArrayList(authorizationTokenDao.listTokenByTeam(team.getTeamId()));
    assertFalse(tokens.isEmpty());
    // get the token and check it was created
    var getTokenResponse = authorizationTokenService.get(response.getAuthorizationToken());
    assertEquals(getTokenResponse.getAuthorizationToken(), response.getAuthorizationToken());

    // list the token check that it was created
    var matchingToken =
        tokens.stream()
            .filter(t -> t.getAuthorizationToken().equals(response.getAuthorizationToken()))
            .findFirst();
    assertFalse(matchingToken.isEmpty(), "Token should be created and listed");

    // check read and write roles are created
    assertTrue(
        matchingToken
            .get()
            .getAuthorizationRoles()
            .contains(RoleTemplates.getTeamWriterRole(team.getOrgId(), team.getTeamId())));
    assertTrue(
        matchingToken
            .get()
            .getAuthorizationRoles()
            .contains(RoleTemplates.getTeamReaderRole(team.getOrgId(), team.getTeamId())));

    // delete the token
    authorizationTokenService.delete(tempToken, matchingToken.get().getAuthorizationToken());
    var tokensAfterDelete =
        Lists.newArrayList(authorizationTokenDao.listTokenByTeam(team.getTeamId()));
    var matchingTokenAfterDelete =
        tokensAfterDelete.stream()
            .filter(t -> t.getAuthorizationToken().equals(response.getAuthorizationToken()))
            .findFirst();
    assertFalse(matchingTokenAfterDelete.isPresent(), "Token should be deleted");
  }

  @Test
  public void testOnlyGrantedRolesAreAdded_read_not_write() throws UnAuthorizedException {
    var loginToken = tokenManager.issueLoginToken(notTeamAdmin.getUserId());
    var tempToken = tokenManager.issueTemporaryBearerToken(ORG_ID, loginToken);
    var createApiTokenRequest = new CreateApiTokenRequest(true, false);
    var tokenResponse =
        authorizationTokenService.createApiToken(
            tempToken, team.getTeamId(), createApiTokenRequest);
    var token = authorizationTokenDao.findToken(tokenResponse.getAuthorizationToken()).get();
    assertTrue(
        token
            .getAuthorizationRoles()
            .contains(RoleTemplates.getTeamReaderRole(team.getOrgId(), team.getTeamId())));
    assertFalse(
        token
            .getAuthorizationRoles()
            .contains(RoleTemplates.getTeamWriterRole(team.getOrgId(), team.getTeamId())));
  }

  @Test
  public void testOnlyGrantedRolesAreAdded_write_not_read() throws UnAuthorizedException {
    var loginToken = tokenManager.issueLoginToken(notTeamAdmin.getUserId());
    var tempToken = tokenManager.issueTemporaryBearerToken(ORG_ID, loginToken);
    var createApiTokenRequest = new CreateApiTokenRequest(false, true);
    var tokenResponse =
        authorizationTokenService.createApiToken(
            tempToken, team.getTeamId(), createApiTokenRequest);
    var token = authorizationTokenDao.findToken(tokenResponse.getAuthorizationToken()).get();
    assertFalse(
        token
            .getAuthorizationRoles()
            .contains(RoleTemplates.getTeamReaderRole(team.getOrgId(), team.getTeamId())));
    assertTrue(
        token
            .getAuthorizationRoles()
            .contains(RoleTemplates.getTeamWriterRole(team.getOrgId(), team.getTeamId())));
  }

  @Test
  public void testCreateApiTokenAuthorizationTokenIfNotAdmin() throws UnAuthorizedException {
    var loginToken = tokenManager.issueLoginToken(notTeamAdmin.getUserId());
    var tempToken = tokenManager.issueTemporaryBearerToken(ORG_ID, loginToken);
    var notAcceptedRoles =
        List.of(
            RoleTemplates.getTeamWriterRole(team.getOrgId(), team.getTeamId()),
            RoleTemplates.getTeamReaderRole(team.getOrgId(), team.getTeamId()));
    for (var role : notAcceptedRoles) {
      var response =
          authorizationTokenService.createApiToken(
              tempToken, team.getTeamId(), new CreateApiTokenRequest(true, true));
      usersDao.grantRole(notTeamAdmin.getUserId(), role);
    }
    assertThrows(
        UnAuthorizedException.class,
        () -> {
          var response = authorizationTokenService.list(tempToken, team.getTeamId());
        });

    assertThrows(
        UnAuthorizedException.class,
        () -> {
          authorizationTokenService.delete("some-token", team.getTeamId());
        });
  }
}
