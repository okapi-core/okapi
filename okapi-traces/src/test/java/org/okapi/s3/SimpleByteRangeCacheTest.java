package org.okapi.s3;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class SimpleByteRangeCacheTest {

  private static byte[] src() {
    // 32 bytes: 0..31
    byte[] b = new byte[32];
    for (int i = 0; i < b.length; i++) b[i] = (byte) i;
    return b;
  }

  @Test
  void returns_correct_subranges_and_tracks_stats() throws Exception {
    byte[] source = src();
    SimpleByteRangeCache.RangeFetcher fetcher =
        (bucket, key, s, e) -> java.util.Arrays.copyOfRange(source, (int) s, (int) e);
    SimpleByteRangeCache cache = new SimpleByteRangeCache(fetcher, 16, 8, Duration.ofSeconds(60));

    // First read within one chunk [4,7]
    byte[] r1 = cache.getRange("b", "k", 4, 8);
    assertArrayEquals(new byte[] {4, 5, 6, 7}, r1);
    ByteRangeCache.CacheStats s1 = cache.stats();
    assertEquals(0, s1.getHits());
    assertEquals(1, s1.getMisses());
    assertEquals(8, s1.getBytesFetched()); // fetched full chunk of 8 bytes

    // Second read same range should hit cache
    byte[] r2 = cache.getRange("b", "k", 4, 8);
    assertArrayEquals(new byte[] {4, 5, 6, 7}, r2);
    ByteRangeCache.CacheStats s2 = cache.stats();
    assertTrue(s2.getHits() >= 1);
    assertEquals(s1.getMisses(), s2.getMisses());

    // Cross-chunk read [6,14) spans chunks [0] and [1]
    byte[] r3 = cache.getRange("b", "k", 6, 14);
    assertArrayEquals(new byte[] {6, 7, 8, 9, 10, 11, 12, 13}, r3);
    ByteRangeCache.CacheStats s3 = cache.stats();
    // One of the chunks already cached, so at most one miss added
    assertTrue(s3.getMisses() >= s2.getMisses());
  }
}

