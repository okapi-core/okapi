/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.spring.configs.scheduled;

import java.time.Duration;
import org.okapi.runtime.S3UploadScheduler;
import org.okapi.spring.configs.Profiles;
import org.okapi.spring.configs.properties.S3UploadCfg;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class S3UploadJobs implements SchedulingConfigurer {
  S3UploadScheduler s3UploadScheduler;
  S3UploadCfg uploadCfg;

  public S3UploadJobs(S3UploadScheduler s3UploadScheduler, S3UploadCfg uploadCfg) {
    this.s3UploadScheduler = s3UploadScheduler;
    this.uploadCfg = uploadCfg;
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    taskRegistrar.addFixedDelayTask(
        s3UploadScheduler::onTick, Duration.ofMillis(uploadCfg.getUploadDelayMs()));
  }
}
