package org.okapi.web.spring.config;

import org.okapi.data.dao.PendingJobsDao;
import org.okapi.web.service.federation.dispatcher.PendingJobPoller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PendingJobPollerConfig {

  @Bean
  public PendingJobPoller pendingJobPoller(
      @Autowired PendingJobsDao pendingJobsDao, @Autowired PollingTaskCfg taskCfg) {
    return new PendingJobPoller(pendingJobsDao, taskCfg);
  }
}
