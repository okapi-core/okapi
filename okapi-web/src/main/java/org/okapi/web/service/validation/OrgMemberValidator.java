package org.okapi.web.service.validation;

import org.okapi.data.exceptions.ResourceNotFoundException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.web.auth.AccessManager;
import org.okapi.web.auth.TokenManager;
import org.okapi.web.service.context.OrgMemberContext;
import org.springframework.stereotype.Service;

@Service
public class OrgMemberValidator {

  AccessManager accessManager;
  TokenManager tokenManager;

  public OrgMemberValidator(AccessManager accessManager, TokenManager tokenManager) {
    this.accessManager = accessManager;
    this.tokenManager = tokenManager;
  }

  public OrgMemberContext checkOrgMember(String token)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    var userId = tokenManager.getUserId(token);
    var orgId = tokenManager.getOrgId(token);
    accessManager.checkUserHasIsOrgMember(userId, orgId);
    return new OrgMemberContext(userId, orgId);
  }

  public OrgMemberContext checkOrgMatch(String token, String orgId) {
    var tokenOrgId = tokenManager.getOrgId(token);
    if (!tokenOrgId.equals(orgId)) {
      throw new UnAuthorizedException("Not authorized for this organization");
    }
    var userId = tokenManager.getUserId(token);
    return new OrgMemberContext(userId, orgId);
  }

  public OrgMemberContext checkOrgAdmin(String tempToken)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    var userId = tokenManager.getUserId(tempToken);
    var orgId = tokenManager.getOrgId(tempToken);
    accessManager.checkUserHasIsOrgAdmin(userId, orgId);
    return new OrgMemberContext(userId, orgId);
  }
}
