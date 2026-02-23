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
public class GetDashboardPanelResponse {
  @NotNull String panelId;
  String title;
  String description;
  MultiQueryPanelWDto queryConfig;
}
