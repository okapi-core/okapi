package org.okapi.okapi_agent.jobhandler;

import com.google.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.okapi.okapi_agent.scheduled.JobsPoller;

@Slf4j
public class PendingJobRunner {
  public record PendingJobConfig(long delay) {}

  ScheduledExecutorService threadPoolExecutor;
  JobsPoller jobsPoller;
  PendingJobConfig config;

  @Inject
  public PendingJobRunner(PendingJobConfig config, JobsPoller jobsPoller) {
    this.threadPoolExecutor = Executors.newScheduledThreadPool(1);
    this.jobsPoller = jobsPoller;
    this.config = config;
  }

  public void start() {
    this.threadPoolExecutor.scheduleAtFixedRate(
        () -> {
          try {
            jobsPoller.nextPendingJob();
          } catch (Exception e) {
            log.error("Error while polling for pending jobs: ", e);
          }
        },
        0,
        config.delay,
        TimeUnit.MILLISECONDS);
  }

  public void stop() {
    threadPoolExecutor.shutdownNow();
  }
}
