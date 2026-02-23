package org.okapi.web.controller;

import java.util.List;
import org.okapi.web.dtos.dashboards.CreateDashboardRequest;
import org.okapi.web.dtos.dashboards.GetDashboardResponse;
import org.okapi.web.dtos.dashboards.UpdateDashboardRequest;
import org.okapi.web.dtos.dashboards.versions.ListDashboardVersionsResponse;
import org.okapi.web.dtos.dashboards.versions.PublishDashboardVersionRequest;
import org.okapi.web.dtos.dashboards.versions.PublishDashboardVersionResponse;
import org.okapi.web.headers.RequestHeaders;
import org.okapi.web.service.context.DashboardAccessContext;
import org.okapi.web.service.dashboards.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {
  @Autowired DashboardService dashboardService;

  @PostMapping("/dashboards")
  public GetDashboardResponse createDashboard(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @RequestBody @Validated CreateDashboardRequest req)
      throws Exception {
    return dashboardService.create(new DashboardAccessContext(tempToken, null), req);
  }

  @GetMapping("/dashboards")
  public List<GetDashboardResponse> listDashboards(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken) throws Exception {
    return dashboardService.listDashboards(tempToken);
  }

  @GetMapping("/dashboards/{dashboardId}/versions")
  public ListDashboardVersionsResponse listVersions(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("dashboardId") String dashboardId)
      throws Exception {
    return dashboardService.listVersions(tempToken, dashboardId);
  }

  @PostMapping("/dashboards/{dashboardId}/publish")
  public PublishDashboardVersionResponse publishDashboard(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("dashboardId") String dashboardId,
      @RequestBody @Validated PublishDashboardVersionRequest request)
      throws Exception {
    return dashboardService.publishVersion(tempToken, dashboardId, request.getVersionId());
  }

  @GetMapping("/dashboards/{dashboardId}/versions/{versionId}")
  public GetDashboardResponse getDashboardVersion(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("dashboardId") String dashboardId,
      @PathVariable("versionId") String versionId)
      throws Exception {
    return dashboardService.readVersion(tempToken, dashboardId, versionId);
  }

  @GetMapping("/dashboards/{dashboardId}/versions/active")
  public GetDashboardResponse getDashboardActiveVersion(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("dashboardId") String dashboardId)
      throws Exception {
    return dashboardService.read(new DashboardAccessContext(tempToken, dashboardId));
  }

  @PostMapping("/dashboards/{dashboardId}/delete")
  public void deleteDashboard(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("dashboardId") String dashboardId)
      throws Exception {
    dashboardService.delete(new DashboardAccessContext(tempToken, dashboardId));
  }

  @PostMapping("/dashboards/{dashboardId}/update")
  public GetDashboardResponse updateDashboard(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @PathVariable("dashboardId") String dashboardId,
      @RequestBody @Validated UpdateDashboardRequest req)
      throws Exception {
    return dashboardService.update(new DashboardAccessContext(tempToken, dashboardId), req);
  }
}
