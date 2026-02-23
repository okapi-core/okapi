package org.okapi.web.service.dashboards.rows;

public class DashboardRowIdParser {
  public static final DashboardRowId parse(String rowFqId)
      throws org.okapi.exceptions.BadRequestException {
    return new DashboardRowId(rowFqId);
  }
}
