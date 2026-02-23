package org.okapi.web.dtos.dashboards.vars;

import lombok.Value;

@Value
public class DeleteDashboardVarRequest {
  String dashboardId;
  String name;
}
