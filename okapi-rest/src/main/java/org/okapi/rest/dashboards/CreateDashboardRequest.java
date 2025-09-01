package org.okapi.rest.dashboards;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class CreateDashboardRequest {
  @NotNull
  String definition;
  @NotNull
  String note;
  @NotNull
  String title;
  boolean orgWideRead;
  boolean orgWideWrite;
  List<String> teamsWithReadAccess;
  List<String> teamsWithWriteAccess;
}
