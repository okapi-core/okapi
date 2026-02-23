/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dto;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.List;

public class PendingJobsStateMachine {
  public static final Multimap<JOB_STATUS, JOB_STATUS> VALID_TRANSITIONS =
      ArrayListMultimap.create();

  static {
    VALID_TRANSITIONS.putAll(
        JOB_STATUS.PENDING, List.of(JOB_STATUS.IN_PROGRESS, JOB_STATUS.CANCELLED));
    VALID_TRANSITIONS.putAll(
        JOB_STATUS.IN_PROGRESS,
        List.of(JOB_STATUS.COMPLETED, JOB_STATUS.FAILED, JOB_STATUS.CANCELLED));
    VALID_TRANSITIONS.putAll(JOB_STATUS.COMPLETED, List.of());
    VALID_TRANSITIONS.putAll(JOB_STATUS.FAILED, List.of(JOB_STATUS.PENDING));
    VALID_TRANSITIONS.putAll(JOB_STATUS.CANCELLED, List.of());
  }
  ;

  public static boolean canTransition(JOB_STATUS from, JOB_STATUS to) {
    return VALID_TRANSITIONS.containsEntry(from, to);
  }

  // DAO layer performs conditional update; state machine only validates transitions.
}
