/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import okhttp3.OkHttpClient;
import org.okapi.okapi_agent.jobhandler.JobHandlerModule;
import org.okapi.okapi_agent.jobhandler.PendingJobRunner;
import org.okapi.okapi_agent.scheduled.JobsPoller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

  @Bean
  public Injector getInjector() {
    return Guice.createInjector(new JobHandlerModule());
  }

  @Bean
  public OkHttpClient okHttpClient(@Autowired Injector injector) {
    return injector.getInstance(OkHttpClient.class);
  }

  @Bean
  public JobsPoller jobsPoller(@Autowired Injector injector) {
    return injector.getInstance(JobsPoller.class);
  }

  @Bean
  public PendingJobRunner jobRunner(@Autowired Injector injector) {
    var runner = injector.getInstance(PendingJobRunner.class);
    runner.start();
    return runner;
  }
}
