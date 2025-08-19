package org.okapi.auth;

import org.okapi.data.exceptions.UserAlreadyExistsException;
import org.okapi.fixtures.Deduplicator;
import org.okapi.metrics.IdCreationFailedException;
import com.okapi.rest.auth.AuthRequest;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.fixtures.SingletonFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.okapi.auth.TestCommons.addToOrg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TokenManagerIntegrationTest {
  SingletonFactory singletonFactory;

  public static final String ORG_ID =
      Deduplicator.dedup("org-12345", TokenManagerIntegrationTest.class);

  @BeforeEach
  public void setup() {
    // Initialize the necessary components for the TokenManager
    // This could include setting up mock objects or real instances
    // depending on the context of the integration test.
    singletonFactory = new SingletonFactory();
  }

  @Test
  public void testTokenChainIssued()
      throws BadRequestException,
          UnAuthorizedException,
          IdCreationFailedException,
          UserAlreadyExistsException {
    // create a random new user, issue login and temp token, both should be valid
    var randomEmail = "testuser_" + System.currentTimeMillis() + "@example.com";
    var authRequest = new AuthRequest("Oscar", "Okapi", randomEmail, "password123");
    var token = singletonFactory.getUserManager().signupWithEmailPassword(authRequest);
    addToOrg(singletonFactory, ORG_ID, randomEmail, true);
    var tempToken = singletonFactory.tokenManager().issueTemporaryBearerToken(ORG_ID, token);
    var userIdClaim = singletonFactory.tokenManager().getUserId(tempToken);

    var userDto = singletonFactory.usersDao().get(userIdClaim);
    assertTrue(userDto.isPresent(), "User should exist in the database after signup");
    assertEquals(
        randomEmail,
        userDto.get().getEmail(),
        "User email should match the one used during signup");
  }
}
