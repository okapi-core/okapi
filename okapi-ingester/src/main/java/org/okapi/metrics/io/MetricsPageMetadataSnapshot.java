/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.io;

import com.google.common.hash.BloomFilter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MetricsPageMetadataSnapshot {
  BloomFilter<Integer> metricNameTrigrams;
  BloomFilter<Integer> tagPatternTrigrams;
}
