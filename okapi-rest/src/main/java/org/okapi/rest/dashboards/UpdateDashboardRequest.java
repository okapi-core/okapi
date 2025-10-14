package org.okapi.rest.dashboards;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UpdateDashboardRequest {
  String definition;
  String note;
  String title;
  boolean orgWideReadAccess;
  boolean orgWideWriteAccess;
  List<String> teamsWithReadAccess;
  List<String> teamsWithWriteAccess;
}
