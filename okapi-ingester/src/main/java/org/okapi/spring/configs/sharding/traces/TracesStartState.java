package org.okapi.spring.configs.sharding.traces;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TracesStartState {
  boolean startedShardListener = false;
  boolean bootstrappedConsumer = false;
}
