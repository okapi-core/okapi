package org.okapi.wal.it.observers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.okapi.wal.WalCommitContext;
import org.okapi.wal.WalCommitListener;

public final class CommitHookRecorder implements WalCommitListener {
  private final List<WalCommitContext> events = new ArrayList<>();

  @Override
  public synchronized void onWalCommit(WalCommitContext ctx) {
    events.add(ctx);
  }

  public synchronized List<WalCommitContext> events() {
    return Collections.unmodifiableList(events);
  }

  public synchronized long lastLsn() {
    return events.isEmpty() ? -1L : events.get(events.size() - 1).getLsn();
  }
}
