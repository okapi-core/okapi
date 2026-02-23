package org.okapi.metrics.service.fakes;

import java.util.concurrent.*;
import lombok.Setter;

public class ImmediateScheduler extends ScheduledThreadPoolExecutor {
  ScheduledThreadPoolExecutor futureExecutor;
  @Setter int delay = 100;

  public ImmediateScheduler(int corePoolSize) {
    super(corePoolSize);
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    // Immediately execute the command instead of scheduling it
    return super.schedule(command, this.delay, TimeUnit.MILLISECONDS);
  }
}
