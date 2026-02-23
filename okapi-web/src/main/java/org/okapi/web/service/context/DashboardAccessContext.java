package org.okapi.web.service.context;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DashboardAccessContext {
  String token;
  String dashboardId;

  public static DashboardAccessContext of(String token, String dashboardId) {
    return new DashboardAccessContext(token, dashboardId);
  }

  public static DashboardAccessContext of(String token) {
    return new DashboardAccessContext(token, null);
  }
}
