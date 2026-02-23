package org.okapi.spring.configs.sharding.logs;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LogsStartState {
  boolean startedShardListener = false;
  boolean bootstrappedConsumer = false;
}
