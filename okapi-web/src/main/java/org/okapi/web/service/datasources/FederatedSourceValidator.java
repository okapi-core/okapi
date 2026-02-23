/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.datasources;

import org.okapi.data.exceptions.ResourceNotFoundException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.web.dtos.sources.CreateFederatedSourceRequest;
import org.okapi.web.service.ProtectedResourceContext;
import org.okapi.web.service.validation.OrgMemberValidator;
import org.okapi.web.service.validation.RequestValidator;
import org.springframework.stereotype.Service;

@Service
public class FederatedSourceValidator
    implements RequestValidator<
        ProtectedResourceContext, FederatedResourceContext, CreateFederatedSourceRequest, Void> {
  OrgMemberValidator orgMemberValidator;

  public FederatedSourceValidator(OrgMemberValidator orgMemberValidator) {
    this.orgMemberValidator = orgMemberValidator;
  }

  @Override
  public FederatedResourceContext validateCreate(
      ProtectedResourceContext context, CreateFederatedSourceRequest request)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    var orgMemberCtx = orgMemberValidator.checkOrgMember(context.getToken());
    return new FederatedResourceContext(orgMemberCtx, null);
  }

  @Override
  public FederatedResourceContext validateRead(ProtectedResourceContext context)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    var orgMemberCtx = orgMemberValidator.checkOrgMember(context.getToken());
    return new FederatedResourceContext(orgMemberCtx, context.getResourceIdOrThrow());
  }

  @Override
  public FederatedResourceContext validateUpdate(ProtectedResourceContext context, Void request)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    var orgMemberCtx = orgMemberValidator.checkOrgMember(context.getToken());
    return new FederatedResourceContext(orgMemberCtx, context.getResourceIdOrThrow());
  }

  @Override
  public FederatedResourceContext validateDelete(ProtectedResourceContext context)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    var orgMemberCtx = orgMemberValidator.checkOrgMember(context.getToken());
    return new FederatedResourceContext(orgMemberCtx, context.getResourceIdOrThrow());
  }
}
