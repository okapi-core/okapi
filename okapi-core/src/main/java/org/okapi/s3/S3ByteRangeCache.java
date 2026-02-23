/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.s3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Locked;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class S3ByteRangeCache {

  final Long thresholdBytes;
  Map<String, DataPage> byteRangeCache = new ConcurrentHashMap<>();
  Map<String, Long> lastAccessed = new HashMap<>();
  TreeMap<Long, Set<String>> accessTimes = new TreeMap<>();
  long totalCached = 0L;

  public String getCacheKey(String bucket, String key, long start, long end) {
    return bucket + ":" + key + ":" + start + "-" + end;
  }

  @Locked.Write
  public byte[] possiblyCache(
      String bucket, String key, long start, long end, byte[] contents, String etag) {
    if (end <= start) {
      throw new IllegalArgumentException("End must be greater than start");
    }
    var cacheKey = getCacheKey(bucket, key, start, end);
    updateAccessTime(cacheKey);
    if (byteRangeCache.containsKey(cacheKey)) {
      return contents;
    }
    evict();
    byteRangeCache.put(cacheKey, new DataPage(contents, etag));
    totalCached += contents.length;
    return contents;
  }

  private Optional<String> getEvictionTarget() {
    var oldestEntry = accessTimes.firstKey();
    if (oldestEntry == null) {
      return Optional.empty();
    }
    var candidates = accessTimes.get(oldestEntry);
    if (candidates == null || candidates.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(candidates.iterator().next());
  }

  private void evict() {
    if (totalCached >= thresholdBytes) {
      getEvictionTarget().ifPresent(this::evictKey);
    }
  }

  private void evictKey(String cacheKey) {
    var evicted = byteRangeCache.remove(cacheKey);
    if (evicted != null) {
      totalCached -= evicted.contents().length;
      var accessed = lastAccessed.remove(cacheKey);
      accessTimes.get(accessed).remove(cacheKey);
      if (accessTimes.get(accessed).isEmpty()) {
        accessTimes.remove(accessed);
      }
    }
  }

  private void updateAccessTime(String cacheKey) {
    var oldAccessTime = lastAccessed.get(cacheKey);
    if (oldAccessTime != null) {
      accessTimes.get(oldAccessTime).remove(cacheKey);
      if (accessTimes.get(oldAccessTime).isEmpty()) {
        accessTimes.remove(oldAccessTime);
      }
    }
    long newAccessTime = System.nanoTime();
    lastAccessed.put(cacheKey, newAccessTime);
    accessTimes.computeIfAbsent(newAccessTime, k -> new HashSet<>()).add(cacheKey);
  }

  @Locked.Read
  public Optional<byte[]> getCachedRange(
      String bucket, String key, long start, long end, String etag) {
    String cacheKey = getCacheKey(bucket, key, start, end);
    var val = byteRangeCache.get(cacheKey);
    if (val != null && val.etag().equals(etag)) {
      updateAccessTime(cacheKey);
      return Optional.of(val.contents());
    } else if (val != null) {
      evictKey(cacheKey);
      return Optional.empty();
    } else {
      return Optional.empty();
    }
  }

  public record DataPage(byte[] contents, String etag) {}
}
