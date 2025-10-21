package org.okapi.swim.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "okapi.swim")
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class SwimConfig {
  private int k;
  private long timeoutMillis;
  private int retries;
  private int threadPoolSize;
  // Minimum successes required among K for quorum
  private int mQuorum;
  // Max time to wait for dissemination quorum (ms)
  private long broadcastTimeoutMillis;
  // Initial TTL for re-gossip hop count
  private int gossipHopCount;
  // Time after which a suspected node is marked DEAD (ms)
  private long suspectTimeoutMillis;
  // Dedupe cache configuration
  private long dedupeTtlMillis;
  private int dedupeMaxEntries;
}
