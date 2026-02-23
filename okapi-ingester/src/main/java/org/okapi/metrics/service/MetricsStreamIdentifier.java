package org.okapi.metrics.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.streams.StreamIdentifier;

@Getter
@AllArgsConstructor
public class MetricsStreamIdentifier implements StreamIdentifier<String> {
  String streamId;
}
