package org.okapi.wal.it.env;

import java.io.IOException;
import org.okapi.wal.PersistedLsnStore;

/** Minimal helper to emulate snapshot write + persisted watermark update. */
public final class Snapshots {
  private Snapshots() {}

  public static void writeSnapshotAndPersist(WalTestEnv env, long lsn) throws IOException {
    // Pretend to write & fsync snapshot elsewhere; then update persisted watermark.
    PersistedLsnStore store = env.persistedLsnStore();
    store.updateIfGreater(lsn);
  }
}
