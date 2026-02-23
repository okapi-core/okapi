package org.okapi.web.service.federation.dispatcher;

public interface PollingInnerTask<T> {
  PollingTask.PollStatus<T> run();
}
