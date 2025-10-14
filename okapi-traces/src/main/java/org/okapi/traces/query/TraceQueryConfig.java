package org.okapi.traces.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TraceQueryConfig {
  @Builder.Default private final int queryThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
}

