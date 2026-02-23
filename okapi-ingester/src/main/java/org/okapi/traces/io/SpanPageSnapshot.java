/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.abstractio.TimeRangeSnapshot;

@AllArgsConstructor
@Getter
public class SpanPageSnapshot implements TimeRangeSnapshot {
  SpanPageMetadataSnapshot metadataSnapshot;
  SpanPageBodySnapshot bodySnapshot;

  @Override
  public long getTsStart() {
    return metadataSnapshot.getTsStart();
  }

  @Override
  public long getTsEnd() {
    return metadataSnapshot.getTsEnd();
  }

  @Override
  public int getNDocs() {
    return bodySnapshot.getSpans().size();
  }
}
