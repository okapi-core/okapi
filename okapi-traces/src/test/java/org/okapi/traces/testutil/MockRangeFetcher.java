package org.okapi.traces.testutil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.okapi.traces.query.S3TraceQueryProcessor;

/**
 * Simple in-memory mock for S3 range fetching that can be reused across tests.
 */
public class MockRangeFetcher implements S3TraceQueryProcessor.RangeFetcher {
  private final Map<String, byte[]> files = new ConcurrentHashMap<>();
  private final Map<String, Boolean> fail = new ConcurrentHashMap<>();

  private static String key(String bucket, String key) {
    return bucket + "::" + key;
  }

  public void putFile(String bucket, String key, byte[] fileBytes) {
    files.put(key(bucket, key), fileBytes);
  }

  public void setFail(String bucket, String key, boolean shouldFail) {
    fail.put(key(bucket, key), shouldFail);
  }

  @Override
  public byte[] getRange(String bucket, String key, long start, long endExclusive) {
    String k = key(bucket, key);
    if (fail.getOrDefault(k, false)) throw new RuntimeException("fetch failed for " + k);
    byte[] file = files.get(k);
    if (file == null) return null;
    if (start >= file.length) return null;
    int from = (int) Math.max(0, start);
    int to = (int) Math.min(file.length, Math.max(start, endExclusive));
    if (from >= to) return null;
    byte[] out = new byte[to - from];
    System.arraycopy(file, from, out, 0, out.length);
    return out;
  }

  public java.util.List<String> list(String bucket, String prefix) {
    String pfx = key(bucket, prefix);
    java.util.List<String> keys = new java.util.ArrayList<>();
    for (String k : files.keySet()) {
      if (k.startsWith(pfx)) {
        // strip the bucket:: prefix to return the object key portion
        keys.add(k.substring((bucket + "::").length()));
      }
    }
    return keys;
  }
}
