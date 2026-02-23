package org.okapi.web.dtos.dashboards.vars;

import lombok.Value;

@Value
public class CreateDashboardVarRequest {
  String dashboardId;
  DASH_VAR_TYPE dashVarType;
  String name;
  String tag;
}
