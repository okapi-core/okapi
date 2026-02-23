/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.controller;

import jakarta.validation.Valid;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.headers.CookiesAndHeaders;
import org.okapi.metrics.IdCreationFailedException;
import org.okapi.web.auth.UserManager;
import org.okapi.web.dtos.auth.*;
import org.okapi.web.headers.RequestHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UserController {

  @Autowired UserManager userManager;

  @Value("${cookies.secure}")
  boolean secure;

  @PostMapping("/users")
  public TokenResponse createUser(@RequestBody CreateUserRequest request)
      throws IdCreationFailedException, BadRequestException {
    return new TokenResponse(userManager.signupWithEmailPassword(request));
  }

  @PostMapping("/users/uid-token")
  public TokenResponse signInUser(@RequestBody @Valid SignInRequest request)
      throws UnAuthorizedException {
    return new TokenResponse(userManager.signInWithEmailPassword(request));
  }

  @PostMapping("/users/sign-in")
  public ResponseEntity<String> signInWithPass(@RequestBody @Valid SignInRequest request)
      throws UnAuthorizedException {
    var response = userManager.signInWithEmailPassword(request);
    var cookie =
        ResponseCookie.from(CookiesAndHeaders.COOKIE_LOGIN_TOKEN, response)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(Duration.of(1, ChronoUnit.DAYS))
            .build();
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
  }

  @PostMapping("/users/sign-out")
  public ResponseEntity<Void> signOut() {
    var cookie =
        ResponseCookie.from(CookiesAndHeaders.COOKIE_LOGIN_TOKEN, "dummy")
            .httpOnly(true)
            .sameSite("None")
            .secure(secure)
            .path("/")
            .maxAge(Duration.of(1, ChronoUnit.DAYS))
            .build();
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
  }

  @PostMapping("/auth/{orgId}/session")
  public TokenResponse createTempToken(
      @PathVariable("orgId") String orgId,
      @CookieValue(RequestHeaders.LOGIN_TOKEN) String loginToken)
      throws UnAuthorizedException {
    return userManager.getSessionToken(loginToken, orgId);
  }

  @GetMapping("/users/profile")
  public GetUserProfileResponse getUserProfile(
      @CookieValue(RequestHeaders.LOGIN_TOKEN) String loginToken) throws UnAuthorizedException {
    return userManager.getUserProfileRes(loginToken);
  }

  @PostMapping("/users/profile/update")
  public GetUserProfileResponse updateUserProfile(
      @CookieValue(RequestHeaders.LOGIN_TOKEN) String loginToken,
      @RequestBody UpdateUserRequest updateUserRequest)
      throws UnAuthorizedException, BadRequestException {
    return userManager.updateProfile(loginToken, updateUserRequest);
  }
}
