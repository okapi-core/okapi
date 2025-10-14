package org.okapi.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.fixtures.Deduplicator.dedup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.data.exceptions.UserAlreadyExistsException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.NotFoundException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.fixtures.SingletonFactory;
import org.okapi.metrics.IdCreationFailedException;
import org.okapi.rest.auth.AuthRequest;
import org.okapi.usermessages.UserFacingMessages;

public class UserManagerIntegrationTest {

  UserManager userManager;
  SingletonFactory resourceFactory;

  @BeforeEach
  public void setup() {
    // Initialize the necessary components for the UserManager
    // This could include setting up mock objects or real instances
    // depending on the context of the integration test.
    resourceFactory = new SingletonFactory();
    userManager = resourceFactory.getUserManager();
  }

  @Test
  public void testSignup()
      throws BadRequestException,
          UnAuthorizedException,
          IdCreationFailedException,
          UserAlreadyExistsException,
          NotFoundException {
    var randomEmail = dedup("email_" + System.currentTimeMillis() + "@domain.com", this.getClass());
    var authRequest = new AuthRequest("Oscar", "Okapi", randomEmail, "password123");
    var token = userManager.signupWithEmailPassword(authRequest);
    var userOrgs = userManager.listUserOrgs(token);
    assertFalse(userOrgs.getOrgs().isEmpty(), "User should have at least");
    var orgId = userOrgs.getOrgs().get(0).getOrgId();
    var tempToken = resourceFactory.tokenManager().issueTemporaryBearerToken(orgId, token);
    assertFalse(userOrgs.getOrgs().isEmpty(), "User should have at least");

    var teams = resourceFactory.teamsManager().listTeams(token);
    assertFalse(teams.getTeams().isEmpty(), "User should have at least one team");

    var defaultTeam = teams.getTeams().get(0);
    assertNotNull(defaultTeam.getTeamId(), "Default team should have a valid ID");
    assertTrue(defaultTeam.isAdmin(), "User should be an admin of the default team");

    var org = resourceFactory.orgManager().getOrg(tempToken, userOrgs.getOrgs().get(0).getOrgId());
    assertNotNull(org, "User should be able to retrieve their organization");

    var fetched = resourceFactory.orgDao().findById(org.getOrgId()).get();
    var user = resourceFactory.usersDao().getWithEmail(randomEmail).get();
    assertTrue(fetched.getOrgCreator().equals(user.getUserId()));
  }

  @Test
  public void testSignupWithDuplicateEmail()
      throws BadRequestException,
          IdCreationFailedException,
          UserAlreadyExistsException,
          UnAuthorizedException {
    var randomEmail = dedup("email_" + System.currentTimeMillis() + "@domain.com", this.getClass());
    var authRequest = new AuthRequest("Oscar", "Okapi", randomEmail, "password123");
    var token = userManager.signupWithEmailPassword(authRequest);
    try {
      var anotherToken = userManager.signupWithEmailPassword(authRequest);
      assertFalse(true);
    } catch (BadRequestException e) {
      assertEquals(UserFacingMessages.USER_ALREADY_EXISTS, e.getMessage());
    } catch (UserAlreadyExistsException e) {
      assertTrue(true);
    }
  }
}
