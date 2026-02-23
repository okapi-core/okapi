/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.runtime.spans;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.okapi.spring.configs.Profiles;
import org.okapi.traces.TracesBufferPool;
import org.okapi.traces.config.TracesCfg;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@Component
@Slf4j
public class TracesFlushScheduler {
  private final TracesBufferPool pool;
  private final TracesCfg tracesCfg;

  public TracesFlushScheduler(TracesBufferPool pool, TracesCfg tracesCfg) {
    this.pool = pool;
    this.tracesCfg = tracesCfg;
  }

  public void onTick() {
    Instant now = Instant.now();
    pool.flushPagesOlderThan(
        now.minus(tracesCfg.getIdxExpiryDuration(), ChronoUnit.MILLIS).toEpochMilli());
  }
}
