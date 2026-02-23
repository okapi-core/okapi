/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.access;

import lombok.RequiredArgsConstructor;
import org.okapi.web.auth.AccessManager;
import org.okapi.web.auth.TokenManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrgMemberChecker {

  final TokenManager tokenManager;
  final AccessManager accessManager;

  public OrgAccessContext checkUserIsOrgMember(String tempToken) {
    var userId = tokenManager.getUserId(tempToken);
    var orgId = tokenManager.getOrgId(tempToken);
    accessManager.checkUserIsOrgMember(userId, orgId);
    return OrgAccessContext.builder().orgId(orgId).userId(userId).build();
  }
}
