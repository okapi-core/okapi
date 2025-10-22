package org.okapi.logs;

public class StaticConfiguration {
  public static final int N_BLOCKS = 32; // ~3% of pods are sampled of results at query-time.
  public static final int HASH_BASE = 31;

  public static int rkHash(String target) {
    var hash = 0;
    for (char c : target.toCharArray()) {
      hash = (HASH_BASE * hash + c) % N_BLOCKS;
    }
    return hash;
  }

  public static int hashLogStream(String tenantId, String logStream, long hr) {
    var hash = rkHash(tenantId);
    hash = (31 * hash + rkHash(logStream)) % N_BLOCKS;
    hash = Math.toIntExact((31 * hash + hr) % N_BLOCKS);
    return hash;
  }

  public static int hashNode(String nodeId) {
    return rkHash(nodeId);
  }
}
