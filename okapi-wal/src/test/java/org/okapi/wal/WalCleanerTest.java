package org.okapi.wal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class WalCleanerTest {

  @TempDir Path tmp;

  @Test
  void deletesEligibleSealedSegments_immediatePurge() throws Exception {
    // Create segments 1..6; mark 1..4 sealed with different LSNs; 5 sealed but not eligible; 6
    // active
    createSegment(tmp, 1, 10, 19, true);
    createSegment(tmp, 2, 20, 29, true);
    createSegment(tmp, 3, 30, 39, true);
    createSegment(tmp, 4, 40, 45, true); // <= persisted watermark
    createSegment(tmp, 5, 50, 59, true);
    createSegment(tmp, 6, 60, 69, false); // active

    // Persisted watermark at 45 (covers 1..4)
    PersistedLsnStore store = PersistedLsnStore.open(tmp);
    store.write(45L);

    WalCleaner cleaner =
        new WalCleaner(
            tmp, Clock.systemUTC(), Duration.ZERO, /*keepLastKSealed*/ 0, /*dryRun*/ false);
    cleaner.run();

    // Expect 1..4 deleted; 5,6 remain
    assertThat(Files.exists(tmp.resolve("wal_0000000001.segment"))).isFalse();
    assertThat(Files.exists(tmp.resolve("wal_0000000002.segment"))).isFalse();
    assertThat(Files.exists(tmp.resolve("wal_0000000003.segment"))).isFalse();
    assertThat(Files.exists(tmp.resolve("wal_0000000004.segment"))).isFalse();

    assertThat(Files.exists(tmp.resolve("wal_0000000005.segment"))).isTrue();
    assertThat(Files.exists(tmp.resolve("wal_0000000006.segment"))).isTrue();
  }

  @Test
  void respectsKeepLastKSealed_retainsNewestEligible() throws Exception {
    createSegment(tmp, 1, 10, 19, true);
    createSegment(tmp, 2, 20, 29, true);
    createSegment(tmp, 3, 30, 39, true);
    createSegment(tmp, 4, 40, 49, true);

    PersistedLsnStore store = PersistedLsnStore.open(tmp);
    store.write(100L); // all sealed segments eligible by watermark

    WalCleaner cleaner =
        new WalCleaner(tmp, Clock.systemUTC(), Duration.ZERO, /*keepLastKSealed*/ 2, false);
    cleaner.run();

    // Expect oldest 2 deleted, newest 2 retained
    assertThat(Files.exists(tmp.resolve("wal_0000000001.segment"))).isFalse();
    assertThat(Files.exists(tmp.resolve("wal_0000000002.segment"))).isFalse();
    assertThat(Files.exists(tmp.resolve("wal_0000000003.segment"))).isTrue();
    assertThat(Files.exists(tmp.resolve("wal_0000000004.segment"))).isTrue();
  }

  @Test
  void quarantineThenPurge_afterGrace() throws Exception {
    createSegment(tmp, 1, 10, 19, true);

    PersistedLsnStore store = PersistedLsnStore.open(tmp);
    store.write(100L);

    Instant t0 = Instant.parse("2025-08-15T00:00:00Z");
    Clock c0 = Clock.fixed(t0, ZoneOffset.UTC);
    WalCleaner cleaner = new WalCleaner(tmp, c0, Duration.ofMinutes(30), 0, false);

    cleaner.run();

    // After first run, file should be in trash (not in root)
    assertThat(Files.exists(tmp.resolve("wal_0000000001.segment"))).isFalse();
    Path trashRoot = tmp.resolve(".wal_trash");
    assertThat(Files.isDirectory(trashRoot)).isTrue();
    // there should be one batch dir
    Path batch = Files.list(trashRoot).findFirst().orElseThrow();
    assertThat(Files.exists(batch.resolve("wal_0000000001.segment"))).isTrue();

    // Advance clock past grace and run again → purge
    Clock c1 = Clock.fixed(t0.plus(Duration.ofMinutes(31)), ZoneOffset.UTC);
    WalCleaner cleaner2 = new WalCleaner(tmp, c1, Duration.ofMinutes(30), 0, false);
    cleaner2.run();

    assertThat(Files.exists(trashRoot)).isFalse(); // batch removed, trash dir removed if empty
  }

  @Test
  void doesNotDeleteActive_unsealed() throws Exception {
    createSegment(tmp, 1, 10, 19, true);
    createSegment(tmp, 2, 20, 29, false); // active

    PersistedLsnStore store = PersistedLsnStore.open(tmp);
    store.write(100L);

    WalCleaner cleaner = new WalCleaner(tmp, Clock.systemUTC(), Duration.ZERO, 0, false);
    cleaner.run();

    assertThat(Files.exists(tmp.resolve("wal_0000000002.segment"))).isTrue();
  }

  // --- helpers ---

  private static void createSegment(Path root, int epoch, long minLsn, long maxLsn, boolean sealed)
      throws Exception {
    Path seg = root.resolve(String.format("wal_%010d.segment", epoch));
    Files.createFile(seg);
    SegmentIndex idx = new SegmentIndex(epoch);
    idx.minLsn = minLsn;
    idx.maxLsn = maxLsn;
    idx.recordCount = (maxLsn >= minLsn) ? (maxLsn - minLsn + 1) : 0;
    idx.sealed = sealed;
    idx.store(seg);
  }
}
