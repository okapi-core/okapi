/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.dashboards.rows;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.web.service.context.OrgMemberContext;

@AllArgsConstructor
@Getter
public class DashboardRowContext {
  OrgMemberContext orgMember;
  String rowFqId;
}
