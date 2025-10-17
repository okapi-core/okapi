package org.okapi.s3;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple chunked byte-range cache. Chunks are fixed-size and keyed by (bucket,key,chunkIndex).
 * Backed by a Guava Cache with maxSize and expiry. Fetches use a provided RangeFetcher.
 */
public class SimpleByteRangeCache implements ByteRangeCache {

  @FunctionalInterface
  public interface RangeFetcher {
    byte[] fetch(String bucket, String key, long startInclusive, long endExclusive) throws IOException;
  }

  private final long chunkSize;
  private final Cache<String, byte[]> chunks;
  private final RangeFetcher fetcher;
  private final AtomicLong hits = new AtomicLong();
  private final AtomicLong misses = new AtomicLong();
  private final AtomicLong bytesFromCache = new AtomicLong();
  private final AtomicLong bytesFetched = new AtomicLong();
  private final AtomicLong evictions = new AtomicLong();
  private final ConcurrentMap<String, Boolean> resident = new ConcurrentHashMap<>();

  public SimpleByteRangeCache(RangeFetcher fetcher, int maxChunks, long chunkSize, Duration expiry) {
    this.fetcher = Objects.requireNonNull(fetcher);
    this.chunkSize = chunkSize > 0 ? chunkSize : 64 * 1024;
    this.chunks =
        CacheBuilder.newBuilder()
            .maximumSize(maxChunks)
            .expireAfterAccess(expiry.toMillis(), TimeUnit.MILLISECONDS)
            .removalListener(
                n -> {
                  if (n.wasEvicted()) evictions.incrementAndGet();
                  resident.remove((String) n.getKey());
                })
            .build();
  }

  @Override
  public byte[] getRange(String bucket, String key, long startInclusive, long endExclusive)
      throws IOException {
    if (endExclusive <= startInclusive)
      throw new IllegalArgumentException("end must be > start");
    int nBytes = Math.toIntExact(endExclusive - startInclusive);
    byte[] out = new byte[nBytes];

    long pageStartIdx = startInclusive / chunkSize;
    long pageEndIdx = (endExclusive - 1) / chunkSize;

    long srcOffset = startInclusive;
    int dstOffset = 0;
    for (long idx = pageStartIdx; idx <= pageEndIdx; idx++) {
      long chunkStart = idx * chunkSize;
      long chunkEnd = chunkStart + chunkSize;
      String ckey = cacheKey(bucket, key, idx);
      byte[] chunk = chunks.getIfPresent(ckey);
      if (chunk == null) {
        long fetchStart = chunkStart;
        long fetchEnd = chunkEnd;
        byte[] fetched = fetcher.fetch(bucket, key, fetchStart, fetchEnd);
        chunks.put(ckey, fetched);
        resident.put(ckey, Boolean.TRUE);
        misses.incrementAndGet();
        bytesFetched.addAndGet(fetched.length);
        chunk = fetched;
      } else {
        hits.incrementAndGet();
      }

      int copyStart = (int) Math.max(0, srcOffset - chunkStart);
      int copyLen = (int) Math.min(chunkEnd - srcOffset, endExclusive - (startInclusive + dstOffset));
      System.arraycopy(chunk, copyStart, out, dstOffset, copyLen);
      if (chunk != null) bytesFromCache.addAndGet(copyLen);
      dstOffset += copyLen;
      srcOffset += copyLen;
    }

    return out;
  }

  private static String cacheKey(String bucket, String key, long chunkIndex) {
    return bucket + "::" + key + "::" + chunkIndex;
  }

  @Override
  public void invalidate(String bucket, String key) {
    String prefix = bucket + "::" + key + "::";
    for (String k : resident.keySet()) {
      if (k.startsWith(prefix)) chunks.invalidate(k);
    }
  }

  @Override
  public void invalidateAll() {
    chunks.invalidateAll();
    resident.clear();
  }

  @Override
  public CacheStats stats() {
    return new CacheStats(hits.get(), misses.get(), bytesFromCache.get(), bytesFetched.get(),
        evictions.get(), 0L);
  }

  @Override
  public void close() {
    invalidateAll();
  }
}

