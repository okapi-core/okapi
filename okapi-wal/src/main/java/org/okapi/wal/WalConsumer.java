package org.okapi.wal;

import java.nio.file.Path;

public interface WalConsumer<Batch> {
  void consume(Batch record);

  void snapshot(Path path);
}
