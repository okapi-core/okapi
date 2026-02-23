/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.datasources;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.web.service.context.OrgMemberContext;

@AllArgsConstructor
@Getter
public class FederatedResourceContext {
  OrgMemberContext memberContext;
  String sourceId;
}
