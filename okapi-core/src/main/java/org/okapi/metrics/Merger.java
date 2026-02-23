/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics;

import org.okapi.metrics.stats.UpdatableStatistics;

public interface Merger<T extends UpdatableStatistics> {

  UpdatableStatistics merge(T A, T B);
}
