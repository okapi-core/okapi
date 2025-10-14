package org.okapi.traces.page;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TraceWriterConfig {
  @Builder.Default private final long idleCloseMillis = 60_000L; // 60s
  @Builder.Default private final long reapIntervalMillis = 15_000L; // 15s
}
