/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.dtos.dashboards;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class CreateDashboardPanelRequest {
  @NotNull String orgId;
  @NotNull String dashboardId;
  @NotNull String versionId;
  @NotNull String rowId;
  @NotNull String panelId;
  String title;
  String note;
  @NotNull List<PanelQueryConfigWDto> queryConfig;
}
