package org.okapi.metrics.rocks;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.testutils.OkapiTestUtils;
import org.rocksdb.RocksDBException;

public class RocksStoreTests {

  @TempDir Path tempPath;

  @Test
  public void testCreateRocksWriter() throws IOException, RocksDBException {
    var rocksStore = new RocksStore();
    var writer = rocksStore.rocksWriter(tempPath);
    var reader = rocksStore.rocksReader(tempPath);
    assertTrue(reader.isPresent());
    var key = "key";
    var value = "val";
    writer.put(key.getBytes(), value.getBytes());

    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(
            () -> {
              return OkapiTestUtils.bytesAreEqual(
                  reader.get().get(key.getBytes()), value.getBytes());
            });
  }

  @Test
  public void testDeleted() throws IOException, RocksDBException {
    var rocksStore = new RocksStore();
    var reader = rocksStore.rocksReader(tempPath);
    assertTrue(reader.isPresent());
    var writer = rocksStore.rocksWriter(tempPath);
    var tMinus2 = System.currentTimeMillis() - Duration.of(2, ChronoUnit.HOURS).toMillis();
    var tMinus1 = System.currentTimeMillis() - Duration.of(1, ChronoUnit.HOURS).toMillis();
    var t = System.currentTimeMillis();
    var key1 = tMinus2 + ":" + "mpa2:tenant2";
    var key2 = tMinus1 + ":" + "mp1:tenant1";
    var key3 = t + ":" + "metric-name-large:tenant0";

    writer.put(key1.getBytes(), key1.getBytes());
    writer.put(key2.getBytes(), key2.getBytes());
    writer.put(key3.getBytes(), key3.getBytes());

    for (var k : Arrays.asList(key1, key2, key3)) {
      await()
          .atMost(Duration.of(1, ChronoUnit.SECONDS))
          .until(() -> OkapiTestUtils.bytesAreEqual(reader.get().get(k.getBytes()), k.getBytes()));
    }

    var rocks = writer.rocksDB;
    var deleteRange = Arrays.<byte[]>asList(("" + tMinus2).getBytes(), ("" + tMinus1).getBytes());
    rocks.deleteFilesInRanges(null, deleteRange, true);
    rocks.compactRange();
    assertNull(reader.get().get(Long.toString(tMinus1).getBytes()));
    assertNull(reader.get().get(Long.toString(tMinus2).getBytes()));
  }
}
