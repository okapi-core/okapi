package org.okapi.web.dtos.dashboards;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class CreateDashboardRowRequest {
  @NotNull String orgId;
  @NotNull String dashboardId;
  @NotNull String versionId;
  @NotNull String rowId;
  String title;
  String description;
}
