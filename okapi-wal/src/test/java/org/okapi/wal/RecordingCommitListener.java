package org.okapi.wal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordingCommitListener implements WalCommitListener {

  private final List<WalCommitContext> events = new ArrayList<>();
  private boolean throwOnCall = false;

  public void setThrowOnCall(boolean throwOnCall) {
    this.throwOnCall = throwOnCall;
  }

  @Override
  public void onWalCommit(WalCommitContext ctx) {
    if (throwOnCall) {
      throw new RuntimeException("Injected listener failure");
    }
    events.add(ctx);
  }

  public List<WalCommitContext> events() {
    return Collections.unmodifiableList(events);
  }
}
