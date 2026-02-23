/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.spring.configs.scheduled;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.okapi.logs.config.LogsCfg;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.runtime.logs.LogsFlushScheduler;
import org.okapi.runtime.metrics.MetricsFlushScheduler;
import org.okapi.runtime.spans.TracesFlushScheduler;
import org.okapi.spring.configs.Profiles;
import org.okapi.traces.config.TracesCfg;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class BufferPoolFlushJobs implements SchedulingConfigurer {
  private final LogsFlushScheduler logsFlushScheduler;
  private final LogsCfg logsCfg;
  private final TracesCfg tracesCfg;
  private final TracesFlushScheduler tracesFlushScheduler;
  private final MetricsCfg metricsCfg;
  private final MetricsFlushScheduler metricsFlushScheduler;

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    // assuming you add this field to MetricsCfg and bind it from
    // okapi.metrics.bufferPool.flush.evalMillis
    taskRegistrar.addFixedDelayTask(
        logsFlushScheduler::onTick, Duration.ofMillis(logsCfg.getBufferPoolFlushEvalMillis()));
    taskRegistrar.addFixedDelayTask(
        tracesFlushScheduler::onTick, Duration.ofMillis(tracesCfg.getBufferPoolFlushEvalMillis()));
    taskRegistrar.addFixedDelayTask(
        metricsFlushScheduler::onTick,
        Duration.ofMillis(metricsCfg.getBufferPoolFlushEvalMillis()));
  }
}
