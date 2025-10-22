package org.okapi.swim.hash;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.okapi.swim.ping.Member;

public final class RendezvousHasher {
  private RendezvousHasher() {}

  public static Member select(List<Member> candidates, byte[] key) {
    if (candidates == null || candidates.isEmpty()) return null;
    Member best = null;
    long bestScore = Long.MIN_VALUE;
    for (Member m : candidates) {
      long score = score(key, m.getNodeId());
      if (score > bestScore) {
        bestScore = score;
        best = m;
      }
    }
    return best;
  }

  public static long score(byte[] key, String nodeId) {
    Objects.requireNonNull(nodeId, "nodeId");
    byte[] nodeBytes = nodeId.getBytes(StandardCharsets.UTF_8);
    // Simple 64-bit mix of key and node bytes
    long h = 1125899906842597L; // prime seed
    for (byte b : key) h = 31 * h + b;
    for (byte b : nodeBytes) h = 31 * h + b;
    // final avalanche
    h ^= (h >>> 33);
    h *= 0xff51afd7ed558ccdL;
    h ^= (h >>> 33);
    h *= 0xc4ceb9fe1a85ec53L;
    h ^= (h >>> 33);
    return h;
  }

  public static byte[] key(String tenantId, String logStream, long hourStartMillis, int blockIdx) {
    byte[] a = tenantId.getBytes(StandardCharsets.UTF_8);
    byte[] b = logStream.getBytes(StandardCharsets.UTF_8);
    ByteBuffer buf = ByteBuffer.allocate(a.length + b.length + 8 + 4);
    buf.put(a).put(b).putLong(hourStartMillis).putInt(blockIdx);
    return buf.array();
  }
}

