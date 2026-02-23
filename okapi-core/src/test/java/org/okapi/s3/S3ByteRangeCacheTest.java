package org.okapi.s3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class S3ByteRangeCacheTest {
  @Test
  void testEvictionRemovesOldest_twoEntries() {
    var cached = new S3ByteRangeCache(10L);
    cached.possiblyCache("bucket", "a", 0, 10, new byte[10], "abc");
    cached.possiblyCache("bucket", "b", 0, 10, new byte[10], "abc");
    assertTrue(cached.getCachedRange("bucket", "a", 0, 10, "abc").isEmpty());
  }

  @Test
  void testEvictionRemovesOldest_threeEntries() {
    var cached = new S3ByteRangeCache(10L);
    cached.possiblyCache("bucket", "a", 0, 10, new byte[10], "abc");
    cached.possiblyCache("bucket", "b", 0, 10, new byte[10], "abc");
    cached.possiblyCache("bucket", "c", 0, 10, new byte[10], "abc");
    assertTrue(cached.getCachedRange("bucket", "a", 0, 10, "abc").isEmpty());
    assertTrue(cached.getCachedRange("bucket", "b", 0, 10, "abc").isEmpty());
    assertTrue(cached.getCachedRange("bucket", "c", 0, 10, "abc").isPresent());
  }

  @Test
  void testOnlyOldestRemoved() {
    var cached = new S3ByteRangeCache(20L);
    cached.possiblyCache("bucket", "a", 0, 10, new byte[10], "abc");
    cached.possiblyCache("bucket", "b", 0, 10, new byte[10], "abc");
    cached.possiblyCache("bucket", "c", 0, 10, new byte[10], "abc");
    assertTrue(cached.getCachedRange("bucket", "a", 0, 10, "abc").isEmpty());
    assertTrue(cached.getCachedRange("bucket", "b", 0, 10, "abc").isPresent());
    assertTrue(cached.getCachedRange("bucket", "c", 0, 10, "abc").isPresent());
  }

  @Test
  void testNoEvictionNeeded() {
    var cached = new S3ByteRangeCache(30L);
    cached.possiblyCache("bucket", "a", 0, 10, new byte[10], "abc");
    cached.possiblyCache("bucket", "b", 0, 10, new byte[10], "abc");
    cached.possiblyCache("bucket", "c", 0, 10, new byte[10], "abc");
    assertTrue(cached.getCachedRange("bucket", "a", 0, 10, "abc").isPresent());
    assertTrue(cached.getCachedRange("bucket", "b", 0, 10, "abc").isPresent());
    assertTrue(cached.getCachedRange("bucket", "c", 0, 10, "abc").isPresent());
  }

  @Test
  void testReadingProtectsFromEviction() {
    var cached = new S3ByteRangeCache(20L);
    cached.possiblyCache("bucket", "a", 0, 10, new byte[10], "abc");
    cached.possiblyCache("bucket", "b", 0, 10, new byte[10], "abc");
    cached.getCachedRange("bucket", "a", 0, 10, "abc");
    cached.possiblyCache("bucket", "c", 0, 10, new byte[10], "abc");
    assertTrue(cached.getCachedRange("bucket", "a", 0, 10, "abc").isPresent());
    assertTrue(cached.getCachedRange("bucket", "b", 0, 10, "abc").isEmpty());
    assertTrue(cached.getCachedRange("bucket", "c", 0, 10, "abc").isPresent());
  }

  @Test
  void testDuplicatesNotCounted() {
    var cached = new S3ByteRangeCache(20L);
    cached.possiblyCache("bucket", "a", 0, 10, new byte[10], "abc");
    cached.possiblyCache("bucket", "a", 0, 10, new byte[10], "abc");
    cached.possiblyCache("bucket", "b", 0, 10, new byte[10], "abc");
    cached.possiblyCache("bucket", "b", 0, 10, new byte[10], "abc");
    assertTrue(cached.getCachedRange("bucket", "a", 0, 10, "abc").isPresent());
    assertTrue(cached.getCachedRange("bucket", "b", 0, 10, "abc").isPresent());
  }
}
