/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics;

import java.util.List;
import lombok.Value;
import org.okapi.metrics.stats.RolledUpStatistics;

@Value
public class CondensedReading {
  List<Integer> ts;
  List<RolledUpStatistics> vals;
}
