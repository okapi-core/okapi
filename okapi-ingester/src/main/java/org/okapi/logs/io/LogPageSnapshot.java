/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.abstractio.TimeRangeSnapshot;

@AllArgsConstructor
public class LogPageSnapshot implements TimeRangeSnapshot {
  @Getter LogPageMetadataSnapshot logPageMetadataSnapshot;
  @Getter LogBodySnapshot logBodySnapshot;

  public long getTsStart() {
    return logPageMetadataSnapshot.getTsStart();
  }

  public long getTsEnd() {
    return logPageMetadataSnapshot.getTsEnd();
  }

  public int getNDocs() {
    return logBodySnapshot.getLogDocs().size();
  }
}
