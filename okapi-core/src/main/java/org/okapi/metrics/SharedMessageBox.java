package org.okapi.metrics;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SharedMessageBox<T> {
  BlockingQueue<T> pending;

  public SharedMessageBox(int maxPending){
    pending = new ArrayBlockingQueue<>(maxPending);
  }

  public void push(T request, String id) throws InterruptedException {
    this.pending.put(request);
  }

  public void drain(Collection<T> sink, String id) {
    this.pending.drainTo(sink);
  }

  public boolean isEmpty(){
    return pending.isEmpty();
  }
}
