/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.fixtures.Deduplicator.dedup;

import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.data.dao.OrgDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.metrics.IdCreationFailedException;
import org.okapi.usermessages.UserFacingMessages;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.okapi.web.dtos.org.OrgMemberWDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.CONCURRENT)
public class UserManagerIT extends AbstractIT {

  @Autowired UserManager userManager;
  @Autowired TokenManager tokenManager;
  @Autowired OrgManager orgManager;
  @Autowired OrgDao orgDao;
  @Autowired UsersDao usersDao;

  String randomEmail = dedup("email@domain.com", this.getClass());
  String secondEmail = dedup("email2@domain.com", this.getClass());

  @BeforeEach
  public void setup() throws UnAuthorizedException {
    super.setup();
  }

  @Test
  public void testSignup()
      throws BadRequestException, UnAuthorizedException, IdCreationFailedException {
    var authRequest = new CreateUserRequest("Oscar", "Okapi", randomEmail, "password123");
    var token = userManager.signupWithEmailPassword(authRequest);
    var userOrgs = orgManager.listOrgs(token);
    assertFalse(userOrgs.getOrgs().isEmpty(), "User should have at least one org.");
    var orgId = userOrgs.getOrgs().get(0).getOrgId();
    var tempToken = tokenManager.issueTemporaryToken(token, orgId);
    assertFalse(userOrgs.getOrgs().isEmpty(), "User should have at least");

    var org = orgManager.getOrg(tempToken, userOrgs.getOrgs().get(0).getOrgId());
    assertNotNull(org, "User should be able to retrieve their organization");

    var user = usersDao.getWithEmail(randomEmail).get();

    var orgInfo = orgManager.getOrg(tempToken, orgId);
    assertEquals(orgId, orgInfo.getOrgId(), "Retrieved org ID should match.");
    var orgMemberIds =
        orgInfo.getMembers().stream().map(OrgMemberWDto::getEmail).collect(Collectors.toSet());
    assertTrue(orgMemberIds.contains(randomEmail));
  }

  @Test
  public void testSignupWithDuplicateEmail() throws BadRequestException, IdCreationFailedException {
    var authRequest = new CreateUserRequest("Oscar", "Okapi", secondEmail, "password123");
    var token = userManager.signupWithEmailPassword(authRequest);
    try {
      var anotherToken = userManager.signupWithEmailPassword(authRequest);
      assertFalse(true);
    } catch (BadRequestException e) {
      assertEquals(UserFacingMessages.USER_ALREADY_EXISTS, e.getMessage());
    }
  }
}
