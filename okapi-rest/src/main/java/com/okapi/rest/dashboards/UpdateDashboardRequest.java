package com.okapi.rest.dashboards;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

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
