package org.okapi.wal;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * WAL cleaner that deletes sealed segments whose maxLSN <= persistedLSN. Two-phase: move to trash,
 * then purge trash older than gracePeriod.
 *
 * <p>Assumes segment sidecars (.idx) are maintained by the writer.
 */
public final class WalCleaner {

  private static final Pattern SEGMENT_PATTERN = Pattern.compile("^wal_\\d{10}\\.segment$");
  private static final String TRASH_DIR = ".wal_trash";

  private final Path root;
  private final Clock clock;
  private final Duration gracePeriod;
  private final int keepLastKSealed;
  private final boolean dryRun;

  public WalCleaner(
      Path root, Clock clock, Duration gracePeriod, int keepLastKSealed, boolean dryRun) {
    this.root = root;
    this.clock = clock;
    this.gracePeriod = gracePeriod;
    this.keepLastKSealed = Math.max(0, keepLastKSealed);
    this.dryRun = dryRun;
  }

  public void run() throws IOException {
    // Acquire cleaner lock
    try (WalProcessLock lock =
        WalProcessLock.acquire(
            root,
            "cleaner",
            Map.of("role", "cleaner", "started_at", Long.toString(clock.millis())))) {

      // Read persisted LSN
      PersistedLsnStore store = PersistedLsnStore.open(root);
      long persisted = store.read();
      if (persisted < 0) {
        // nothing to do safely
        return;
      }

      // 1) discover segments and indices
      List<SegmentView> segments = listSegments();

      // 2) filter: only SEALED with maxLsn <= persisted
      List<SegmentView> eligible =
          segments.stream()
              .filter(s -> s.sealed)
              .filter(s -> s.maxLsn != Long.MIN_VALUE)
              .filter(s -> s.maxLsn <= persisted)
              .sorted(Comparator.comparingInt(s -> s.epoch))
              .collect(Collectors.toList());

      // 3) keep last K sealed as cushion
      if (eligible.size() > keepLastKSealed) {
        eligible = eligible.subList(0, eligible.size() - keepLastKSealed);
      } else {
        eligible = List.of();
      }

      // 4) move eligible to trash
      if (!eligible.isEmpty()) {
        Path trashRoot = root.resolve(TRASH_DIR);
        Files.createDirectories(trashRoot);
        Path batchDir = trashRoot.resolve(Long.toString(clock.millis()));
        Files.createDirectories(batchDir);

        for (SegmentView s : eligible) {
          if (dryRun) continue;

          // atomic move with fallback (segment)
          Path targetSeg = batchDir.resolve(s.segmentPath.getFileName().toString());
          try {
            Files.move(s.segmentPath, targetSeg, REPLACE_EXISTING, ATOMIC_MOVE);
          } catch (AtomicMoveNotSupportedException e) {
            Files.move(s.segmentPath, targetSeg, REPLACE_EXISTING);
          }

          // sidecar if present
          if (Files.exists(s.indexPath)) {
            Path targetIdx = batchDir.resolve(s.indexPath.getFileName().toString());
            try {
              Files.move(s.indexPath, targetIdx, REPLACE_EXISTING, ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
              Files.move(s.indexPath, targetIdx, REPLACE_EXISTING);
            }
          }
        }
      }

      // 5) purge trash older than grace
      if (!dryRun) {
        purgeOldTrash();
      }
    }
  }

  private void purgeOldTrash() throws IOException {
    Path trashRoot = root.resolve(TRASH_DIR);
    if (!Files.isDirectory(trashRoot)) return;

    Instant cutoff = Instant.now(clock).minus(gracePeriod);
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(trashRoot)) {
      for (Path child : ds) {
        if (!Files.isDirectory(child)) continue;
        // dir name is epoch millis; if not parseable, fall back to mtime
        Instant created;
        try {
          created = Instant.ofEpochMilli(Long.parseLong(child.getFileName().toString()));
        } catch (NumberFormatException nfe) {
          created = Files.getLastModifiedTime(child).toInstant();
        }
        if (created.isBefore(cutoff) || gracePeriod.isZero() || gracePeriod.isNegative()) {
          // delete recursively
          deleteRecursively(child);
        }
      }
    }

    try (DirectoryStream<Path> remaining = Files.newDirectoryStream(trashRoot)) {
      if (!remaining.iterator().hasNext()) {
        Files.deleteIfExists(trashRoot);
      }
    }
  }

  private static void deleteRecursively(Path dir) throws IOException {
    if (!Files.exists(dir)) return;
    List<Path> all = new ArrayList<>();
    try (var walk = Files.walk(dir)) {
      walk.forEach(all::add);
    }
    // delete files first, then dirs
    all.sort(Comparator.comparingInt(p -> -p.getNameCount()));
    for (Path p : all) {
      Files.deleteIfExists(p);
    }
  }

  private List<SegmentView> listSegments() throws IOException {
    List<SegmentView> out = new ArrayList<>();
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
      for (Path p : ds) {
        String name = p.getFileName().toString();
        if (!SEGMENT_PATTERN.matcher(name).matches()) continue;
        int epoch = SegmentIndex.parseEpochFromSegment(p);
        Path idx = SegmentIndex.indexPathFor(p);
        SegmentIndex si = SegmentIndex.loadOrNew(p);
        out.add(new SegmentView(epoch, p, idx, si.minLsn, si.maxLsn, si.sealed));
      }
    }
    return out;
  }

  private static final class SegmentView {
    final int epoch;
    final Path segmentPath;
    final Path indexPath;
    final long minLsn;
    final long maxLsn;
    final boolean sealed;

    SegmentView(
        int epoch, Path segmentPath, Path indexPath, long minLsn, long maxLsn, boolean sealed) {
      this.epoch = epoch;
      this.segmentPath = segmentPath;
      this.indexPath = indexPath;
      this.minLsn = minLsn;
      this.maxLsn = maxLsn;
      this.sealed = sealed;
    }
  }
}
