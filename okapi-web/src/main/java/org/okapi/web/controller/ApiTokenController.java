/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.controller;

import org.okapi.web.dtos.token.ApiToken;
import org.okapi.web.dtos.token.GetTokenResponse;
import org.okapi.web.dtos.token.ListTokensResponse;
import org.okapi.web.dtos.token.UpdateTokenRequest;
import org.okapi.web.headers.RequestHeaders;
import org.okapi.web.service.token.ApiTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tokens")
public class ApiTokenController {

  @Autowired ApiTokenService apiTokenService;

  @GetMapping("")
  public ListTokensResponse listTokens(@RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken) {
    return apiTokenService.listTokens(tempToken);
  }

  @PostMapping("/update")
  public GetTokenResponse updateToken(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @RequestBody @Validated UpdateTokenRequest updateTokenRequest) {
    return apiTokenService.updateToken(tempToken, updateTokenRequest);
  }

  @PostMapping("")
  public ApiToken createToken(@RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken) {
    return apiTokenService.createApiSourceToken(tempToken);
  }
}
