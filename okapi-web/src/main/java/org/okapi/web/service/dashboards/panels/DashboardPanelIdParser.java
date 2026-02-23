/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.dashboards.panels;

import org.okapi.exceptions.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class DashboardPanelIdParser {
  public DashboardPanelId parse(String panelFqId)
      throws IllegalArgumentException, BadRequestException {
    return parseStatic(panelFqId);
  }

  public static final DashboardPanelId parseStatic(String panelFqId)
      throws IllegalArgumentException, BadRequestException {
    return new DashboardPanelId(panelFqId);
  }
}
