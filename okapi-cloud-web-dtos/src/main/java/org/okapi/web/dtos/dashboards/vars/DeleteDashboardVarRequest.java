/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.dtos.dashboards.vars;

import lombok.Value;

@Value
public class DeleteDashboardVarRequest {
  String dashboardId;
  String name;
}
