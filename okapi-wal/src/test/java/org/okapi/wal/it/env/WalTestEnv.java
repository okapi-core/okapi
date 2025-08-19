package org.okapi.wal.it.env;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.okapi.wal.*;

public final class WalTestEnv {
  private final Path root;

  public WalTestEnv(Path root) {
    this.root = root;
  }

  public Path root() {
    return root;
  }

  public WalAllocator allocator() throws IOException {
    return new WalAllocator(root);
  }

  public PersistedLsnStore persistedLsnStore() throws IOException {
    return PersistedLsnStore.open(root);
  }

  public WalCleaner cleaner(
      java.time.Clock clock, java.time.Duration grace, int keepLastK, boolean dryRun) {
    return new WalCleaner(root, clock, grace, keepLastK, dryRun);
  }

  public WalRecoveryScanner recoveryScanner() {
    return new WalRecoveryScanner();
  }

  public Path activeSegmentPath() throws IOException {
    return allocator().active();
  }

  public List<Path> listSegmentsSorted() throws IOException {
    try (Stream<Path> s = Files.list(root)) {
      return s.filter(p -> p.getFileName().toString().matches("^wal_\\d{10}\\.segment$"))
          .sorted(Comparator.comparingInt(org.okapi.wal.SegmentIndex::parseEpochFromSegment))
          .collect(Collectors.toList());
    }
  }

  public SegmentIndex loadIndex(Path seg) throws IOException {
    return SegmentIndex.loadOrNew(seg);
  }

  /** Simulate operator action to remove stale locks after a crash. */
  public void forceUnlocks() throws IOException {
    Files.deleteIfExists(root.resolve("write.lock"));
    Files.deleteIfExists(root.resolve("cleaner.lock"));
  }
}
