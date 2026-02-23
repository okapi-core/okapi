package org.okapi.metrics.io;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MetricsPageSnapshot {
  MetricsPageMetadataSnapshot metadataSnapshot;
  MetricsPageBodySnapshot bodySnapshot;
}
