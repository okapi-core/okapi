/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.paths;

import java.nio.file.Path;
import org.okapi.abstractio.ExpiryDurationPartitionedPaths;

public class LogsDiskPaths extends ExpiryDurationPartitionedPaths<String> {
  public LogsDiskPaths(Path dataDir, long idxExpiryDuration, String baseFileName) {
    super(dataDir, idxExpiryDuration, baseFileName);
  }
}
