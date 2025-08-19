package org.okapi.wal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.okapi.wal.Wal.WalRecord;

public class FakeWalWriter implements WalWriter<WalRecord> {

  private final List<WalRecord> writes = new ArrayList<>();

  @Override
  public void write(WalRecord record) throws IOException {
    writes.add(record);
  }

  public List<WalRecord> records() {
    return Collections.unmodifiableList(writes);
  }
}
