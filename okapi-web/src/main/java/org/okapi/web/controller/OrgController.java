/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.controller;

import static org.okapi.web.headers.RequestHeaders.LOGIN_TOKEN;
import static org.okapi.web.headers.RequestHeaders.TEMP_TOKEN;

import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.NotFoundException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.web.auth.OrgManager;
import org.okapi.web.dtos.org.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class OrgController {

  @Autowired OrgManager orgManager;

  @GetMapping("/orgs")
  public ListOrgsResponse listOrgs(@CookieValue(LOGIN_TOKEN) String loginToken)
      throws UnAuthorizedException {
    return orgManager.listOrgs(loginToken);
  }

  @GetMapping("/orgs/{orgId}")
  public GetOrgResponse getOrg(
      @PathVariable("orgId") String orgId, @RequestHeader(TEMP_TOKEN) String tempToken)
      throws BadRequestException, UnAuthorizedException {
    return orgManager.getOrg(tempToken, orgId);
  }

  @PostMapping("/orgs/{orgId}")
  public GetOrgResponse updateOrg(
      @PathVariable("orgId") String orgId,
      @RequestHeader(TEMP_TOKEN) String tempToken,
      @RequestBody UpdateOrgRequest updateOrgRequest)
      throws BadRequestException, UnAuthorizedException {
    return orgManager.updateOrg(tempToken, orgId, updateOrgRequest);
  }

  @PostMapping("/orgs/{orgId}/members")
  public ResponseEntity<Void> createOrgMember(
      @PathVariable("orgId") String orgId,
      @RequestHeader(TEMP_TOKEN) String tempToken,
      @RequestBody CreateOrgMemberRequest request)
      throws UnAuthorizedException {
    orgManager.createOrgMember(tempToken, request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/orgs/{orgId}/members/update")
  public ResponseEntity<Void> deleteOrgMember(
      @PathVariable("orgId") String orgId,
      @RequestHeader(TEMP_TOKEN) String tempToken,
      @RequestBody UpdateOrgMemberRequest request)
      throws NotFoundException, BadRequestException, UnAuthorizedException {
    orgManager.updateOrgMember(tempToken, orgId, request);
    return ResponseEntity.noContent().build();
  }
}
