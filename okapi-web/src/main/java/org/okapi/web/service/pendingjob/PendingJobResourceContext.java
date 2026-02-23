/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.pendingjob;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.okapi.web.service.context.OrgMemberContext;

@AllArgsConstructor
@Getter
@Builder
public class PendingJobResourceContext {
  OrgMemberContext orgMemberContext;
  String jobId;
}
