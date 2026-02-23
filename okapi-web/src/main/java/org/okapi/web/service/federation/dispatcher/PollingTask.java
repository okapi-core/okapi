/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.federation.dispatcher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;

public class PollingTask<T> implements Runnable {

  public enum POLL_STATUS {
    PENDING,
    DONE
  }

  public record PollStatus<T>(POLL_STATUS status, T result) {}

  public record RunConfig(int maxAttempts, long delayMillis) {}

  AtomicInteger attempts = new AtomicInteger(0);
  @Getter CompletableFuture<T> future = new CompletableFuture<>();
  PollingInnerTask<T> innerTask;
  RunConfig runConfig;
  ScheduledExecutorService scheduler;

  public PollingTask(
      PollingInnerTask<T> innerTask, RunConfig runConfig, ScheduledExecutorService scheduler) {
    this.innerTask = innerTask;
    this.runConfig = runConfig;
    this.scheduler = scheduler;
  }

  @Override
  public void run() {
    try {
      if (attempts.get() >= runConfig.maxAttempts()) {
        future.completeExceptionally(
            new IllegalStateException("Max polling attempts exceeded, the task was not finished."));
        return;
      }
      attempts.incrementAndGet();
      PollStatus<T> pollStatus = innerTask.run();
      if (pollStatus.status() == POLL_STATUS.DONE) {
        future.complete(pollStatus.result());
      } else {
        scheduler.schedule(
            this, runConfig.delayMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
      }
    } catch (Exception e) {
      future.completeExceptionally(e);
    }
  }
}
