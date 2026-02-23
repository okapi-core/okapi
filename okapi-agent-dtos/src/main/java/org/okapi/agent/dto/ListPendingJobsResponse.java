/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.agent.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ListPendingJobsResponse {
  @NotNull @Singular private List<PendingJob> pendingJobs;
}
