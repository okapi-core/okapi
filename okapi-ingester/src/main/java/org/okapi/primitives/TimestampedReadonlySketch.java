/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.primitives;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Getter
public class TimestampedReadonlySketch implements Comparable<TimestampedReadonlySketch> {
  long ts;
  ReadOnlySketch readOnlySketch;

  @Override
  public int compareTo(@NotNull TimestampedReadonlySketch o) {
    return Long.compare(ts, o.ts);
  }
}
