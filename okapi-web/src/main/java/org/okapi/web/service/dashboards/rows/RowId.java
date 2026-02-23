package org.okapi.web.service.dashboards.rows;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RowId {
  String orgId;
  String dashboardId;
  String rowId;
}
