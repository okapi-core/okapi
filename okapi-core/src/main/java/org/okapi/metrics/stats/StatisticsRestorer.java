/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.stats;

public interface StatisticsRestorer<T> {
  T deserialize(byte[] bytes);
}
