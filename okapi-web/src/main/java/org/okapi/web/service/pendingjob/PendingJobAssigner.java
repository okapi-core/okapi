/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.pendingjob;

import java.util.List;
import org.okapi.data.dto.PendingJobDdb;

public interface PendingJobAssigner {
  List<PendingJobDdb> getPendingJobs(String orgId, List<String> sources, int maxJobs);
}
