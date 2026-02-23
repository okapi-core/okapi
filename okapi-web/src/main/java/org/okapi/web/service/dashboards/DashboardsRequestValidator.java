/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.dashboards;

import javax.swing.*;
import org.okapi.data.exceptions.ResourceNotFoundException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.web.dtos.dashboards.CreateDashboardRequest;
import org.okapi.web.dtos.dashboards.UpdateDashboardRequest;
import org.okapi.web.service.context.DashboardAccessContext;
import org.okapi.web.service.validation.OrgMemberValidator;
import org.okapi.web.service.validation.RequestValidator;
import org.springframework.stereotype.Service;

@Service
public class DashboardsRequestValidator
    implements RequestValidator<
        DashboardAccessContext,
        DashboardRequestContext,
        CreateDashboardRequest,
        UpdateDashboardRequest> {

  DashboardAccessValidator accessValidator;
  OrgMemberValidator orgMemberValidator;

  public DashboardsRequestValidator(
      OrgMemberValidator orgMemberValidator, DashboardAccessValidator dashboardAccessValidator) {
    this.orgMemberValidator = orgMemberValidator;
    this.accessValidator = dashboardAccessValidator;
  }

  @Override
  public DashboardRequestContext validateCreate(
      DashboardAccessContext context, CreateDashboardRequest request)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    return DashboardRequestContext.of(orgMemberValidator.checkOrgMember(context.getToken()));
  }

  @Override
  public DashboardRequestContext validateRead(DashboardAccessContext context)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    checkResourceExists(context);
    return new DashboardRequestContext(
        accessValidator.validateRead(context), context.getDashboardId());
  }

  @Override
  public DashboardRequestContext validateUpdate(
      DashboardAccessContext context, UpdateDashboardRequest request)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    checkResourceExists(context);
    return new DashboardRequestContext(
        accessValidator.validateEdit(context), context.getDashboardId());
  }

  @Override
  public DashboardRequestContext validateDelete(DashboardAccessContext context)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    checkResourceExists(context);
    return new DashboardRequestContext(
        accessValidator.validateEdit(context), context.getDashboardId());
  }

  public void checkResourceExists(DashboardAccessContext context) throws ResourceNotFoundException {
    if (context.getDashboardId() == null) {
      throw new ResourceNotFoundException("Resource not found.");
    }
  }
}
