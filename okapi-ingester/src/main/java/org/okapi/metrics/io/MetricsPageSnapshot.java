/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.io;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MetricsPageSnapshot {
  MetricsPageMetadataSnapshot metadataSnapshot;
  MetricsPageBodySnapshot bodySnapshot;
}
