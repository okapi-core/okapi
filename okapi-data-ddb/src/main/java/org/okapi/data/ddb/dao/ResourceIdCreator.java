/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.dao;

import org.okapi.ShortCodes;

public class ResourceIdCreator {
  public static String createResourceId(String... args) {
    if (args == null || args.length == 0) {
      throw new IllegalArgumentException("At least one component is required");
    }
    var sb = new StringBuilder();
    for (int i = 0; i < args.length; i++) {
      var a = args[i];
      if (a == null || a.isEmpty()) {
        throw new IllegalArgumentException("ResourceId part cannot be null/empty at index " + i);
      }
      if (i > 0) sb.append(":");
      sb.append(a);
    }
    return sb.toString();
  }

  public static String createDashboardRn(String orgId, String dashId) {
    var sb = new StringBuilder();
    sb.append(orgId).append(":").append(ShortCodes.DASH).append(":").append(dashId);
    return sb.toString();
  }

  public static String createRowRn(String orgId, String dashId, String rowId) {
    var sb = new StringBuilder();
    sb.append(orgId)
        .append(":")
        .append(ShortCodes.DASH)
        .append(":")
        .append(dashId)
        .append(":")
        .append(ShortCodes.ROW)
        .append(":")
        .append(rowId);
    return sb.toString();
  }

  public static String createPanelRn(String orgId, String dashId, String rowId, String panelId) {
    var sb = new StringBuilder();
    sb.append(orgId)
        .append(":")
        .append(ShortCodes.DASH)
        .append(":")
        .append(dashId)
        .append(":")
        .append(ShortCodes.ROW)
        .append(":")
        .append(rowId)
        .append(":")
        .append(ShortCodes.PANEL)
        .append(panelId);
    return sb.toString();
  }
}
