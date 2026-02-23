package org.okapi.web.service.dashboards;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.web.service.context.OrgMemberContext;

@AllArgsConstructor
@Getter
public class DashboardRequestContext {
  OrgMemberContext orgMemberContext;
  String dashboardId;

  public static final DashboardRequestContext of(
      OrgMemberContext orgMemberContext, String dashboardId) {
    return new DashboardRequestContext(orgMemberContext, dashboardId);
  }

  public static final DashboardRequestContext of(OrgMemberContext orgMemberContext) {
    return new DashboardRequestContext(orgMemberContext, null);
  }
}
