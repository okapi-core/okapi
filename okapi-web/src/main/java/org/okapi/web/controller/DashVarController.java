package org.okapi.web.controller;

import org.okapi.web.dtos.dashboards.vars.CreateDashboardVarRequest;
import org.okapi.web.dtos.dashboards.vars.DeleteDashboardVarRequest;
import org.okapi.web.dtos.dashboards.vars.GetVarResponse;
import org.okapi.web.dtos.dashboards.vars.ListVarsResponse;
import org.okapi.web.headers.RequestHeaders;
import org.okapi.web.service.dashboards.DashboardVarsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class DashVarController {

  @Autowired DashboardVarsService dashboardVarsService;

  @PostMapping("/dashboard-vars")
  public GetVarResponse createVar(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @RequestBody @Validated CreateDashboardVarRequest req)
      throws Exception {
    return dashboardVarsService.createVar(tempToken, req);
  }

  @GetMapping("/dashboard-vars/{dashboardId}")
  public ListVarsResponse listVars(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("dashboardId") String id)
      throws Exception {
    return dashboardVarsService.listVar(tempToken, id);
  }

  @PostMapping("/dashboard-vars/delete")
  public void deleteVar(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @RequestBody @Validated DeleteDashboardVarRequest req)
      throws Exception {
    dashboardVarsService.deleteVar(tempToken, req);
  }
}
