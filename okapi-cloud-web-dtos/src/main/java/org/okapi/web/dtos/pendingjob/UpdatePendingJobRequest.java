/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.dtos.pendingjob;

import lombok.*;
import org.okapi.agent.dto.JOB_STATUS;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
public class UpdatePendingJobRequest {
  String jobId;
  JOB_STATUS status;
}
