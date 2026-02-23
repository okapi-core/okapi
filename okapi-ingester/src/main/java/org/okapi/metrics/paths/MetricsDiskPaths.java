/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.paths;

import java.nio.file.Path;
import org.okapi.abstractio.ExpiryDurationPartitionedPaths;

public class MetricsDiskPaths extends ExpiryDurationPartitionedPaths<String> {
  public MetricsDiskPaths(Path dataDir, long idxExpiryDuration, String baseFileName) {
    super(dataDir, idxExpiryDuration, baseFileName);
  }
}
