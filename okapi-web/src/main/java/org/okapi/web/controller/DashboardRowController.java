/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.controller;

import org.okapi.web.dtos.dashboards.CreateDashboardRowRequest;
import org.okapi.web.dtos.dashboards.GetDashboardRowResponse;
import org.okapi.web.dtos.dashboards.UpdateDashboardRowRequest;
import org.okapi.web.headers.RequestHeaders;
import org.okapi.web.service.ProtectedResourceContext;
import org.okapi.web.service.dashboards.rows.DashboardRowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class DashboardRowController {
  @Autowired DashboardRowService rowService;

  @PostMapping("/panel-rows")
  public GetDashboardRowResponse createRow(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @RequestBody @Validated CreateDashboardRowRequest req)
      throws Exception {
    return rowService.create(ProtectedResourceContext.of(tempToken), req);
  }

  @GetMapping("/panel-rows/{rowId}/versions/{versionId}")
  public GetDashboardRowResponse getRow(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("rowId") String rowId,
      @PathVariable("versionId") String versionId)
      throws Exception {
    return rowService.read(ProtectedResourceContext.of(tempToken, rowId, versionId));
  }

  @PostMapping("/panel-rows/{rowId}/versions/{versionId}/delete")
  public void deleteRow(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("rowId") String rowId,
      @PathVariable("versionId") String versionId)
      throws Exception {
    rowService.delete(ProtectedResourceContext.of(tempToken, rowId, versionId));
  }

  @PostMapping("/panel-rows/{rowId}/update")
  public GetDashboardRowResponse updateRow(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("rowId") String rowId,
      @RequestBody @Validated UpdateDashboardRowRequest req)
      throws Exception {
    return rowService.update(ProtectedResourceContext.of(tempToken, rowId), req);
  }
}
