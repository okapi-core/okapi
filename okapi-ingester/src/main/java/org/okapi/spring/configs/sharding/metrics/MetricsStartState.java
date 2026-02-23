package org.okapi.spring.configs.sharding.metrics;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MetricsStartState {
  boolean startedShardListener = false;
  boolean bootstrappedConsumer = false;
}
