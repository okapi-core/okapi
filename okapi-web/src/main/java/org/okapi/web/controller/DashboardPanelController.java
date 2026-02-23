/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.controller;

import org.okapi.web.dtos.dashboards.CreateDashboardPanelRequest;
import org.okapi.web.dtos.dashboards.GetDashboardPanelResponse;
import org.okapi.web.dtos.dashboards.UpdateDashboardPanelRequest;
import org.okapi.web.headers.RequestHeaders;
import org.okapi.web.service.ProtectedResourceContext;
import org.okapi.web.service.dashboards.panels.DashboardPanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class DashboardPanelController {
  @Autowired DashboardPanelService panelService;

  @PostMapping("/dashboard-panels")
  public GetDashboardPanelResponse createPanel(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @RequestBody @Validated CreateDashboardPanelRequest req)
      throws Exception {
    return panelService.create(ProtectedResourceContext.of(tempToken), req);
  }

  @GetMapping("/dashboard-panels/{panelId}/versions/{versionId}")
  public GetDashboardPanelResponse getPanel(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("panelId") String panelId,
      @PathVariable("versionId") String versionId)
      throws Exception {
    return panelService.read(ProtectedResourceContext.of(tempToken, panelId, versionId));
  }

  @PostMapping("/dashboard-panels/{panelId}/versions/{versionId}/delete")
  public void deletePanel(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("panelId") String panelId,
      @PathVariable("versionId") String versionId)
      throws Exception {
    panelService.delete(ProtectedResourceContext.of(tempToken, panelId, versionId));
  }

  @PostMapping("/dashboard-panels/{panelId}/update")
  public GetDashboardPanelResponse updatePanel(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("panelId") String panelId,
      @RequestBody @Validated UpdateDashboardPanelRequest req)
      throws Exception {
    return panelService.update(ProtectedResourceContext.of(tempToken, panelId), req);
  }
}
