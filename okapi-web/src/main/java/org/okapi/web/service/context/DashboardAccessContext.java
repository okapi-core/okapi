/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
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
