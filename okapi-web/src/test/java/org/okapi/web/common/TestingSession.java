package org.okapi.web.common;

import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;
import org.okapi.data.dao.UsersDao;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.metrics.IdCreationFailedException;
import org.okapi.web.auth.OrgManager;
import org.okapi.web.auth.UserManager;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.okapi.web.dtos.auth.TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

@Service
@ActiveProfiles("test")
public class TestingSession {

  String email;
  String password = "TestPassword123!";

  UserManager userManager;
  OrgManager orgManager;
  UsersDao usersDao;

  public TestingSession(
      @Autowired UserManager userManager,
      @Autowired OrgManager orgManager,
      @Autowired UsersDao usersDao) {
    this.userManager = userManager;
    this.orgManager = orgManager;
    this.usersDao = usersDao;
  }

  @Getter String loginToken;
  @Getter TokenResponse tempToken;

  public void createIfRequired()
      throws IdCreationFailedException, BadRequestException, UnAuthorizedException {
    email = "testuser+" + RandomStringUtils.secure().nextAlphanumeric(5) + "@okapi.org";
    var createUserRequest = CreateUserRequest.builder().email(email).password(password).build();
    var user = usersDao.getWithEmail(email);
    if (user.isEmpty()) {
      userManager.signupWithEmailPassword(createUserRequest);
    }
  }

  public void login() throws UnAuthorizedException {
    loginToken =
        userManager.signInWithEmailPassword(
            org.okapi.web.dtos.auth.SignInRequest.builder()
                .email(email)
                .password(password)
                .build());
  }

  public void getSessionToken() throws UnAuthorizedException {
    var org = orgManager.listOrgs(loginToken);
    var defaultOrg = org.getOrgs().get(0).getOrgId();
    tempToken = userManager.getSessionToken(loginToken, defaultOrg);
  }
}
