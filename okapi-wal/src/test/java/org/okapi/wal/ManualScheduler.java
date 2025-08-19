package org.okapi.wal;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Minimal manual scheduler for tests: - Only supports schedule(Runnable,long,TimeUnit). - Stores
 * runnables; call runNext() to execute. - Other methods are unsupported or no-ops for test usage.
 */
public class ManualScheduler extends AbstractExecutorService implements ScheduledExecutorService {

  private final Queue<Runnable> q = new ArrayDeque<>();
  private volatile boolean shutdown = false;

  public void runNext() {
    Runnable r = q.poll();
    if (r != null) r.run();
  }

  // -------------- ScheduledExecutorService --------------

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    q.add(command);
    return new DummyScheduledFuture();
  }

  @Override
  public <V> ScheduledFuture<V> schedule(
      java.util.concurrent.Callable<V> callable, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(
      Runnable command, long initialDelay, long period, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(
      Runnable command, long initialDelay, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  // -------------- ExecutorService basics --------------

  @Override
  public void shutdown() {
    shutdown = true;
    q.clear();
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown = true;
    List<Runnable> rem = q.stream().toList();
    q.clear();
    return rem;
  }

  @Override
  public boolean isShutdown() {
    return shutdown;
  }

  @Override
  public boolean isTerminated() {
    return shutdown && q.isEmpty();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) {
    return true;
  }

  @Override
  public void execute(Runnable command) {
    q.add(command);
  }

  // -------------- Dummy Future --------------
  private static class DummyScheduledFuture implements ScheduledFuture<Object> {
    @Override
    public long getDelay(TimeUnit unit) {
      return 0;
    }

    @Override
    public int compareTo(Delayed o) {
      return 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return true;
    }

    @Override
    public Object get() {
      return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) {
      return null;
    }
  }
}
