package org.okapi.web.service.dashboards;

import java.util.List;
import lombok.AllArgsConstructor;
import org.okapi.data.dao.DashboardDao;
import org.okapi.data.dao.DashboardVersionDao;
import org.okapi.data.exceptions.ResourceNotFoundException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.web.auth.AccessManager;
import org.okapi.web.auth.TokenManager;
import org.okapi.web.dtos.dashboards.versions.GetDashboardVersionResponse;
import org.okapi.web.dtos.dashboards.versions.ListDashboardVersionsResponse;
import org.okapi.web.dtos.dashboards.versions.PublishDashboardVersionResponse;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DashboardVersionService {
  private final DashboardVersionDao dashboardVersionDao;
  private final DashboardDao dashboardDao;
  private final TokenManager tokenManager;
  private final AccessManager accessManager;

  public ListDashboardVersionsResponse list(String tempToken, String dashboardId)
      throws UnAuthorizedException, ResourceNotFoundException {
    var userId = tokenManager.getUserId(tempToken);
    var orgId = tokenManager.getOrgId(tempToken);
    accessManager.checkUserIsOrgMember(new AccessManager.AuthContext(userId, orgId));
    var dash = dashboardDao.get(orgId, dashboardId);
    if (dash.isEmpty()) {
      throw new ResourceNotFoundException("Dashboard not found: " + dashboardId);
    }
    var versions =
        dashboardVersionDao.list(orgId, dashboardId).stream()
            .map(
                v ->
                    GetDashboardVersionResponse.builder()
                        .dashboardId(v.getDashboardId())
                        .versionId(v.getVersionId())
                        .status(v.getStatus())
                        .createdAt(v.getCreatedAt() == null ? 0 : v.getCreatedAt())
                        .createdBy(v.getCreatedBy())
                        .note(v.getNote())
                        .specHash(v.getSpecHash())
                        .build())
            .toList();
    return ListDashboardVersionsResponse.builder().versions(versions).build();
  }

  public PublishDashboardVersionResponse publish(
      String tempToken, String dashboardId, String versionId)
      throws UnAuthorizedException, ResourceNotFoundException {
    var userId = tokenManager.getUserId(tempToken);
    var orgId = tokenManager.getOrgId(tempToken);
    accessManager.checkUserIsOrgMember(new AccessManager.AuthContext(userId, orgId));
    var dashOpt = dashboardDao.get(orgId, dashboardId);
    if (dashOpt.isEmpty()) {
      throw new ResourceNotFoundException("Dashboard not found: " + dashboardId);
    }
    var versionOpt = dashboardVersionDao.get(orgId, dashboardId, versionId);
    if (versionOpt.isEmpty()) {
      throw new ResourceNotFoundException("Version not found: " + versionId);
    }
    var version = versionOpt.get();
    version.setStatus("PUBLISHED");
    dashboardVersionDao.save(version);

    var dash = dashOpt.get();
    dash.setActiveVersion(versionId);
    dashboardDao.save(dash);

    return PublishDashboardVersionResponse.builder()
        .dashboardId(dashboardId)
        .versionId(versionId)
        .status("PUBLISHED")
        .build();
  }
}
