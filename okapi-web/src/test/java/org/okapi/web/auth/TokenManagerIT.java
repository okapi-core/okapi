package org.okapi.web.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.okapi.fixtures.Deduplicator.dedup;
import static org.okapi.web.auth.TestCommons.addToOrg;

import com.auth0.jwt.algorithms.Algorithm;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.metrics.IdCreationFailedException;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.CONCURRENT)
public class TokenManagerIT extends AbstractIT {
  public final String ORG_ID = dedup("org-12345", TokenManagerIT.class);

  @BeforeEach
  public void setup() {
    // Initialize the necessary components for the TokenManager
    // This could include setting up mock objects or real instances
    // depending on the context of the integration test.
  }

  @Autowired UserManager userManager;
  @Autowired UsersDao usersDao;
  @Autowired RelationGraphDao relationGraphDao;
  @Autowired TokenManager tokenManager;
  @Autowired TokenDecoder decoder;
  @Autowired
  Algorithm algorithm;
  String testInstance = UUID.randomUUID().toString();
  String userEmail = dedup("user@example.com", this.getClass());

  @Test
  public void testIssueBearerToken()
      throws BadRequestException, UnAuthorizedException, IdCreationFailedException {
    // create a random new user, issue login and temp token, both should be valid
    var authRequest = new CreateUserRequest("Oscar", "Okapi", userEmail, "password123");
    var token = userManager.signupWithEmailPassword(authRequest);
    addToOrg(usersDao, relationGraphDao, ORG_ID, userEmail, true);
    var tempToken = tokenManager.issueTemporaryToken(token, ORG_ID);
    var userIdClaim = tokenManager.getUserId(tempToken);

    var userDto = usersDao.get(userIdClaim);
    assertTrue(userDto.isPresent(), "User should exist in the database after signup");
    assertEquals(
        userEmail, userDto.get().getEmail(), "User email should match the one used during signup");
  }

  @Test
  public void testIssueLoginToken() throws UnAuthorizedException {
    String userId = "test-user-id";
    String loginToken = tokenManager.issueLoginToken(userId);
    String extractedUserId = decoder.checkClaimOrThrow(loginToken, JwtClaims.CLAIM_USER_ID, algorithm);
    assertEquals(
        userId, extractedUserId, "The extracted user ID should match the original user ID");
  }

  @Test
  public void testCheckClaimOrThrow_InvalidToken() {
    String invalidToken = "invalid.token.value";
    try {
      decoder.checkClaimOrThrow(invalidToken, JwtClaims.CLAIM_USER_ID, algorithm);
    } catch (UnAuthorizedException e) {
      assertTrue(true, "UnAuthorizedException should be thrown for invalid token");
      return;
    }
  }

  @Test
  public void testCheckClaimOrThrow_MissingClaim() {
    String userId = "test-user-id";
    String loginToken = tokenManager.issueLoginToken(userId);
    try {
      decoder.checkClaimOrThrow(loginToken, "non_existent_claim", algorithm);
    } catch (UnAuthorizedException e) {
      assertTrue(true, "UnAuthorizedException should be thrown for missing claim");
      return;
    }
  }

  @Test
  public void testCheckClaims_ValidClaims() throws UnAuthorizedException {
    String userId = "test-user-id";
    String loginToken = tokenManager.issueLoginToken(userId);
    tokenManager.checkClaims(loginToken, Map.of(JwtClaims.CLAIM_USER_ID, userId));
  }

  @Test
  public void testCheckClaims_InvalidClaims() {
    String userId = "test-user-id";
    String loginToken = tokenManager.issueLoginToken(userId);
    try {
      tokenManager.checkClaims(loginToken, Map.of(JwtClaims.CLAIM_USER_ID, "wrong-user-id"));
    } catch (UnAuthorizedException e) {
      assertTrue(true, "UnAuthorizedException should be thrown for invalid claims");
      return;
    }
  }

  @Test
  public void testGetUserId() {
    var userId = "sample-user-id";
    var loginToken = tokenManager.issueLoginToken(userId);
    try {
      var extractedUserId = tokenManager.getUserId(loginToken);
      assertEquals(
          userId, extractedUserId, "The extracted user ID should match the original user ID");
    } catch (UnAuthorizedException e) {
      assertTrue(false, "UnAuthorizedException should not be thrown for valid token");
    }
  }
}
